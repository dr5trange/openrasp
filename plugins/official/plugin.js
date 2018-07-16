//
// The OpenRASP official plugin has covered certain attack scenarios. The specific detection capabilities should be customized according to the business.
// If you want to know which attacks can be detected and which known vulnerabilities are covered, please refer to the following two links.
//
// Web attack detection capability description, zero rule detection algorithm introduction
// https://rasp.baidu.com/doc/usage/web.html
//
// CVE vulnerability coverage instructions
// https://rasp.baidu.com/doc/usage/cve.html
// 
// OpenRASP best practices
// https://rasp.baidu.com/#section-books
// 
// If you find this plugin can be bypassed, please contact us or submit ISSUE on github
// https://rasp.baidu.com/doc/aboutus/support.html
// 

'use strict'
var plugin  = new RASP('offical')

// Detect logic master switch
// 
// block -&gt; interception
// log -&gt; print the log, do not intercept
// ignore -&gt; turn off this algorithm

var algorithmConfig = {
    // SQL Injection Algorithm #1 - Match User Input
    sqli_userinput: {
        action: 'block'
    },
    // SQL injection algorithm #1 - whether to intercept the database manager, the default is off, there is a need to change to block
    sqli_dbmanager: {
        action: 'ignore'
    },
    // SQL Injection Algorithm #2 - Statement Specification
    sqli_policy: {
        action:  'block',
        feature: {
            // Whether to prohibit multi-statement execution, select ...; update ...;
            'stacked_query':      true,

            // Whether to disable hexadecimal strings, select 0x41424344
            'no_hex':             true,

            // Prohibit version number comments, select/*!500001, 2, */3
            'version_comment':    true,

            // function blacklist, see the list below, select load_file(...)
            'function_blacklist': true,

            // Intercept union select NULL, NULL or union select 1, 2, 3, 4
            'union_null':         true,

            // Whether to disable constant comparison, AND 8333=8555
            // When the code is not standardized, the constant comparison algorithm will cause a lot of false positives, so this feature is no longer enabled by default.
            'constant_compare':   false,
        },
        function_blacklist: {
            // file operation
            'load_file':        true,

            // time difference injection
            'benchmark':        true,
            'sleep':            true,
            'pg_sleep':         true,

            // detection phase
            'is_srvrolemember': true,

            // error injection
            'updatexml':        true,
            'extractvalue':     true,

            // Blind function, if you have a false positive, you can delete some functions.
            'hex':              true,
            'char':             true,
            'chr':              true, 
            'mid':              true,
            'ord':              true,
            'ascii':            true,                
            'bin':              true
        }
    },
    // SSRF - from user input and intercepted for intranet address
    ssrf_userinput: {
        action: 'block'
    },
    // SSRF - whether to allow access to aws metadata
    ssrf_aws: {
        action: 'block'
    },
    // SSRF - Whether to allow access to the dnslog address
    ssrf_common: {
        action:  'block',
        domains: [
            '.ceye.io',
            '.vcap.me',
            '.xip.name',
            '.xip.io',
            '.nip.io',
            '.burpcollaborator.net',
            '.tu4.org'
        ]
    },
    // SSRF - Whether to allow access to the confusing IP address
    ssrf_obfuscate: {
        action: 'block'
    },
    // SSRF - prohibits reading content like file:///etc/passwd using curl
    ssrf_file: {
        action: 'block',
    },

    // arbitrary file download protection - from user input
    readFile_userinput: {
        action: 'block'
    },
    // Any file download protection - use the function such as file_get_contents to read http(s):// content (note, this does not distinguish whether it is an intranet address)
    readFile_userinput_http: {
        action: 'block'
    },
    // arbitrary file download protection - use file:// protocol
    readFile_userinput_file: {
        action: 'block'
    },
    // Any file download protection - use ../../ to jump out of the web directory to read sensitive files
    readFile_traversal: {
        action: 'block'
    },   
    // Any file download protection - read sensitive files, the last line of defense
    readFile_unwanted: {
        action: 'block'
    },

    // Write file operation - NTFS stream
    writeFile_NTFS: {
        action: 'block'
    },
    // Write file operation - PUT upload script file
    writeFile_PUT_script: {
        action: 'block'
    },    
    // Write file operation - script file
    // https://rasp.baidu.com/doc/dev/official.html#case-3
    writeFile_script: {
        action: 'log'
    },

    // Rename monitoring - rename the regular file to webshell,
    // Case has ueditor getshell, MOVE way to upload backdoor, etc.
    rename_webshell: {
        action: 'block'
    },
    // copy_webshell: {
    //     action: 'block'
    // },

    // file manager - reflection mode column directory
    directory_reflect: {
        action: 'block'
    },
    // File Manager - View sensitive directories
    directory_unwanted: {
        action: 'block'
    },
    // File Manager - lists directories other than webroot
    directory_outsideWebroot: {
        action: 'block'
    },

    // file contains - contains http:// content
    include_http: {
        action: 'block'
    },
    // file contains - contains the directory
    include_dir: {
        action: 'block'
    },
    // file contains - contains sensitive files
    include_unwanted: {
        action: 'block'
    },  
    // file contains - contains files outside the web directory
    include_outsideWebroot: {
        action: 'block'
    },

    // XXE - access external entities using unusual protocols such as gopher/ftp/dict/..
    xxe_protocol: {
        action: 'block'
    },

    // File upload - COPY/MOVE mode, only suitable for tomcat
    fileUpload_webdav: {
        action: 'block'
    },
    // File Upload - Multipart Form Mode
    fileUpload_multipart: {
        action: 'block'
    },

    // OGNL code execution vulnerability
    ognl_exec: {
        action: 'block'
    },

    // command execution - reflection, or eval mode
    command_reflect: {
        action: 'block'
    },
    // Command execution - regular mode, if necessary, please change to 'ignore'
    command_other: {
        action: 'block'
    },

    // transformer deserialization attack
    transformer_deser: {
        action: 'block'
    }
}

// Most of OpenRASP's algorithms don't rely on rules. We mainly use the call stack, encoding specification, and user input matching to detect vulnerabilities.
// 
// Currently, only file access - algorithm #4 adds a probe as the last line of defense
// When the application reads these files, it usually means the server has been compromised
// These configurations are generic and generally do not require customization

const clean = {
    action:     'ignore',
    message: 'no risk',
    confidence: 0
}

var forcefulBrowsing = {
    dotFiles: /\.(7z|tar|gz|bz2|xz|rar|zip|sql|db|sqlite)$/,
    nonUserDirectory: /^\/(proc|sys|root)/,

    // webdav file probe - the most frequently downloaded file
    unwantedFilenames: [
        // user files
        '.DS_Store',
        'id_rsa', 'id_rsa.pub', 'known_hosts', 'authorized_keys', 
        '.bash_history', '.csh_history', '.zsh_history', '.mysql_history',

        // project files
        '.htaccess', '.user.ini',

        'web.config', 'web.xml', 'build.property.xml', 'bower.json',
        'Gemfile', 'Gemfile.lock',
        '.gitignore',
        'error_log', 'error.log', 'nohup.out',
    ],

    // directory probe - webshell view the directory with the highest frequency
    unwantedDirectory: [
        '/',
        '/home',
        '/var/log',
        '/private/var/log',
        '/proc',
        '/sys',
        'C:\\',
        'D:\\',
        'E:\\'
    ],

    // file probe - webshell view the file with the highest frequency
    absolutePaths: [
        '/etc/shadow',
        '/etc/passwd',
        '/etc/hosts',
        '/etc/apache2/apache2.conf',
        '/root/.bash_history',
        '/root/.bash_profile',
        'c:\\windows\\system32\\inetsrv\\metabase.xml',
        'c:\\windows\\system32\\drivers\\etc\\hosts'
    ]
}

// If you have configured an unconventional extension mapping, such as letting .abc be executed as a PHP script, you may need to add more extensions.
var scriptFileRegex = /\.(aspx?|jspx?|php[345]?|phtml)\.?$/i

// No other streams are used
var ntfsRegex       = /::\$(DATA|INDEX)$/i

// Commonly used functions
String.prototype.replaceAll = function(token, tokenValue) {
    var index  = 0;
    var string = this;
    
    do {
        string = string.replace(token, tokenValue);
    } while((index = string.indexOf(token, index + 1)) > -1);

    return string
}

// function canonicalPath (path) {
//     return path.replaceAll('/./', '/').replaceAll('//', '/').replaceAll('//', '/')
// }

// We no longer need to simplify the path. When two /../ or two \..\ appear, we can determine the path traversal attack.
// e.g /./././././home/../../../../etc/passwd
function hasTraversal (path) {
    var left  = path.indexOf('/../')
    var right = path.lastIndexOf('/../')

    if (left != -1 && right != -1 && left != right)
    {
        return true
    }

    var left  = path.indexOf('\\..\\')
    var right = path.lastIndexOf('\\..\\')    

    if (left != -1 && right != -1 && left != right)
    {
        return true
    }

    return false
}

function isHostnameDNSLOG(hostname) {
    var domains = algorithmConfig.ssrf_common.domains

    if (hostname == 'requestb.in' || hostname == 'transfer.sh')
    {
        return true
    }
   
    for (var i = 0; i < domains.length; i ++)
    {
        if (hostname.endsWith(domains[i]))
        {
            return true
        }
    }

    return false
}

function basename (path) {
    var idx = path.lastIndexOf('/')
    return path.substr(idx + 1)
}

function validate_stack_php(stacks) {
    var verdict = false

    for (var i = 0; i < stacks.length; i ++) {
        var stack = stacks[i]

        // From  eval/assert/create_function/...

        if (stack.indexOf('eval()\'d code') != -1 
            || stack.indexOf('runtime-created function') != -1
            || stack.indexOf('assert code@') != -1
            || stack.indexOf('regexp code@') != -1) {
            verdict = true
            break
        }

        // There are some false positives, adjust the distance
        if (stack.indexOf('@call_user_func') != -1) {
            if (i <= 3) {
                verdict = true
                break
            }
        }
    }

    return verdict
}

function is_absolute_path(path, os) {

    // Windows - C:\\windows
    if (os == 'Windows') {
            
        if (path[1] == ':')
        {
            var drive = path[0].toLowerCase()
            if (drive >= 'a' && drive <= 'z')
            {
                return true
            }
        }        
    }
    
    // Unices - /root/
    return path[0] === '/'
}

function is_outside_webroot(appBasePath, realpath, path) {
    var verdict = false

    if (realpath.indexOf(appBasePath) == -1 && hasTraversal(path)) {
        verdict = true
    }

    return verdict
}

function is_from_userinput(parameter, target) {
    var verdict = false

    Object.keys(parameter).some(function (key) {
        var value = parameter[key]

        // only handle non-array, hash cases
        if (value[0] == target) {
            verdict = true
            return true
        }
    })

    return verdict
}

// Start

if (RASP.get_jsengine() !== 'v8') {
    // In the java language, in order to improve performance, SQLi / SSRF detection logic changed to java implementation
    // So we need to pass some of the configuration to java
    RASP.config_set('algorithm.config', JSON.stringify(algorithmConfig))
} else {
    // For PHP + V8, performance is not bad, we keep JS detection logic

    // v8 global SQL result cache
    var LRU = {
        cache: {},
        stack: [],
        max:   100,

        lookup: function(key) {
            var found = this.cache.hasOwnProperty(key)
            if (found) {
                var idx = this.stack.indexOf(key)

                this.cache[key] ++
                this.stack.splice(idx, 1)
                this.stack.unshift(key)
            }

            return found
        },

        put: function(key) {
            this.stack.push(key)
            this.cache[key] = 1

            if (this.stack.length > this.max) {
                var tail = this.stack.pop()
                delete this.cache[tail]
            }
        },

        dump: function() {
            console.log (this.cache)
            console.log (this.stack)
            console.log ('')
        }
    }

    plugin.register('sql', function (params, context) {

        // Cache check
        if (LRU.lookup(params.query)) {
            return clean
        }

        var reason     = false
        var parameters = context.parameter || {}
        var tokens     = RASP.sql_tokenize(params.query, params.server)

        // console.log(tokens)

        // Algorithm 1: Match user input
        // 1. Simplely identify if the logic has changed
        // 2. Identify the database manager
        if (algorithmConfig.sqli_userinput.action != 'ignore') {
            Object.keys(parameters).some(function (name) {
                // Cover two cases, the latter only PHP support
                // 
                // ?id=XXXX
                // ?filter[category_id]=XXXX
                var value_list

                if (typeof parameters[name][0] == 'string') {
                    value_list = parameters[name]
                } else {
                    value_list = Object.values(parameters[name][0])
                }

                for (var i = 0; i < value_list.length; i ++) {
                    var value = value_list[i]

                    // Request parameter length is more than 15 to consider, any cross-table query requires at least 20 characters, in fact, can write a larger point
                    // SELECT * FROM admin
                    // and updatexml(....)
                    if (value.length <= 15) {
                        continue
                    }
                   
                    if (value.length == params.query.length && value == params.query) {
                        // Whether to intercept the database manager, please change to 1 if necessary
                        if (algorithmConfig.sqli_dbmanager.action != 'ignore') {
                            reason = 'Algorithm 2: WebShell - Intercept database manager - Attack parameters: ' + name
                            return true
                        } else {
                            continue
                        }
                    }

                    // Simple identification of user input
                    if (params.query.indexOf(value) == -1) {
                        continue
                    }

                    // Remove the user input and match again
                    var tokens2 = RASP.sql_tokenize(params.query.replaceAll(value, ''), params.server)
                    if (tokens.length - tokens2.length > 2) {
                        reason = 'Algorithm 1: Database query logic changed - Attack parameters: ' + name
                        return true
                    }
                }
            })
            if (reason !== false) {
                return {
                    'action':     algorithmConfig.sqli_userinput.action,
                    'confidence': 90,
                    'message':    reason
                }
            }
        }

        // Algorithm 2: SQL statement policy check (simulated SQL firewall function)
        if (algorithmConfig.sqli_policy.action != 'ignore') {
            var features  = algorithmConfig.sqli_policy.feature
            var func_list = algorithmConfig.sqli_policy.function_blacklist

            var tokens_lc = tokens.map(v => v.toLowerCase())

            for (var i = 1; i < tokens_lc.length; i ++) 
            {
                if (features['union_null'] && tokens_lc[i] === 'select') 
                {
                    var null_count = 0

                    // Find consecutive commas, NULLs, or numbers
                    for (var j = i + 1; j < tokens_lc.length && j < i + 6; j ++) {
                        if (tokens_lc[j] === ',' || tokens_lc[j] == 'null' || ! isNaN(parseInt(tokens_lc[j]))) {
                            null_count ++
                        } else {
                            break
                        }
                    }

                    // NULL, NULL, NULL == 5 tokens
                    // 1, 2, 3 == 5 pieces token
                    if (null_count >= 5) {
                        reason = 'UNION-NULL mode injection - field type detection'
                        break
                    }
                    continue
                }

                if (features['stacked_query'] && tokens_lc[i] == ';' && i != tokens_lc.length - 1) 
                {
                    reason = 'Prohibit multi-statement query'
                    break
                } 
                else if (features['no_hex'] && tokens_lc[i][0] === '0' && tokens_lc[i][1] === 'x') 
                {
                    reason = 'ban hex string'
                    break
                } 
                else if (features['version_comment'] && tokens_lc[i][0] === '/' && tokens_lc[i][1] === '*' && tokens_lc[i][2] === '!') 
                {
                    reason = 'Disable MySQL version number comment'
                    break
                } 
                else if (features['constant_compare'] &&
                    i > 0 && i < tokens_lc.length - 1 && 
                    (tokens_lc[i] === 'xor'
                        || tokens_lc[i][0] === '<'
                        || tokens_lc[i][0] === '>' 
                        || tokens_lc[i][0] === '=')) 
                {
                    @FIXME: Can be bypassed, not updated temporarily
                    // Simple recognition of NUMBER (&gt;|&lt;|&gt;=|&lt;=|xor) NUMBER
                    //          i-1         i          i+2    
                        
                    var op1  = tokens_lc[i - 1]
                    var op2  = tokens_lc[i + 1]

                    // @TODO: strip quotes
                    var num1 = parseInt(op1)
                    var num2 = parseInt(op2)

                    if (! isNaN(num1) && ! isNaN(num2)) {
                        // Allow 1=1, 2=0, 201801010=0 such constant comparison to avoid false positives, as long as one is less than 10, ignore it first.
                        // 
                        // SQLmap is a random 4 digit number, unaffected
                        if (tokens_lc[i][0] === '=' && (num1 < 10 || num2 < 10))
                        {
                            continue;
                        }

                        reason = 'Disable constant comparison operation: ' + num1 + ' vs ' + num2
                        break
                    }                    
                } 
                else if (features['function_blacklist'] && i > 0 && tokens_lc[i][0] === '(') 
                {
                    @FIXME: Can be bypassed, not updated temporarily
                    if (func_list[tokens_lc[i - 1]]) {
                        reason = 'Do not execute sensitive functions: ' + tokens_lc[i - 1]
                        break
                    }
                }
            }

            if (reason !== false) {
                return {
                    action:     algorithmConfig.sqli_policy.action,
                    message: 'Algorithm 3: Database statement exception: ' + reason,
                    confidence: 100
                }
            }
        }

        LRU.put(params.query)
        return clean
    })

    plugin.register('ssrf', function (params, context) {
        var hostname = params.hostname
        var url      = params.url
        var ip       = params.ip

        var reason   = false
        var action   = 'ignore'

        // Algorithm 1 - ssrf_userinput
        // When the parameter comes from the user input and is the intranet IP, it is determined to be an SSRF attack.
        if (algorithmConfig.ssrf_userinput.action != 'ignore') 
        {
            if (ip.length &&
                is_from_userinput(context.parameter, url) &&
                /^(192|172|10)\./.test(ip[0]))
            {
                return {
                    action:    algorithmConfig.ssrf_userinput.action,
                    message: 'SSRF attack - access intranet address: ' + ip[0],
                    confidence: 100
                }
            }
        }

        // Algorithm 2 - ssrf_common
        // Check common probe domain names
        if (algorithmConfig.ssrf_common.action != 'ignore')
        {
            if (isHostnameDNSLOG(hostname))
            {
                return {
                    action:    algorithmConfig.ssrf_common.action,
                    message: 'SSRF attack - access to known intranet probe domain name',
                    confidence: 100
                }                
            }
        } 

        // Algorithm 3 - ssrf_aws
        // Detect AWS private address, comment out if required
        // 
        // TODO: Increase the private address of Google Cloud
        if (algorithmConfig.ssrf_aws.action != 'ignore') 
        {
            if (hostname == '169.254.169.254') 
            {
                return {
                    action:    algorithmConfig.ssrf_aws.action,
                    message: 'SSRF attack - read AWS metadata',
                    confidence: 100
                }                
            }
        }

        // Algorithm 4 - ssrf_obfuscate
        // 
        // Check for confusion:
        // http://2130706433
        // http://0x7f001
        // 
        // The following confusion is not detected, it is easy to falsely report
        // http://0x7f.0x0.0x0.0x1
        // http://0x7f.0.0.0    
        if (algorithmConfig.ssrf_obfuscate.action != 'ignore') 
        {
            var reason = false

            if (Number.isInteger(hostname))
            {
                reason = 'Try to use pure numeric IP'
            }
            else if (hostname.startsWith('0x') && hostname.indexOf('.') === -1) 
            {
                reason = 'Try to use hexadecimal IP'
            }

            if (reason)
            {
                return {
                    action:    algorithmConfig.ssrf_obfuscate.action,
                    message: 'SSRF attack - IP address confusion - ' + reason,
                    confidence: 100
                }                
            }
        }

        // Algorithm 5 - ssrf_file
        // 
        // special protocol check, such as
        // read file:///etc/passwd using curl
        if (algorithmConfig.ssrf_file.action != 'ignore')
        {
            var url_lc = url.toLowerCase()
            if (url_lc.startsWith('file://'))
            {
                return {
                    action:    algorithmConfig.ssrf_file.action,
                    message: 'Any file download attack, try to use file:// to read the file',
                    confidence: 100
                }                  
            }
        }

        return clean
    })

}

// Mainly used to identify the file manager in the webshell
// Usually the program does not actively list directories or view sensitive directories, eg /home /etc /var/log etc.
// 
// If there are exceptions to adjust
// Can be combined with business customization: eg can not exceed the application root directory
plugin.register('directory', function (params, context) {
    var path        = params.path
    var realpath    = params.realpath
    var appBasePath = context.appBasePath
    var server      = context.server

    // Algorithm 1 - Read sensitive directories
    if (algorithmConfig.directory_unwanted.action != 'ignore') 
    {
        for (var i = 0; i < forcefulBrowsing.unwantedDirectory.length; i ++) {
            if (realpath == forcefulBrowsing.unwantedDirectory[i]) {
                return {
                    action:     algorithmConfig.directory_unwanted.action,
                    message: 'WebShell File Manager - Read Sensitive Directory',
                    confidence: 100
                }
            }
        }
    }

    // Algorithm 2 - use at least 2 /../ and jump out of the web directory
    if (algorithmConfig.directory_outsideWebroot.action != 'ignore')
    {
        if (hasTraversal(path) && realpath.indexOf(appBasePath) == -1)
        {
            return {
                action:     algorithmConfig.directory_outsideWebroot.action,
                message: 'Try to list directories other than the web directory',
                confidence: 90
            }
        }
    }

    if (algorithmConfig.directory_reflect.action != 'ignore') 
    {

        // Currently, only PHP supports stacking and blocking column directory functions.
        if (server.language == 'php' && validate_stack_php(params.stack)) 
        {
            return {
                action:     algorithmConfig.directory_reflect.action,
                message: 'Discover Webshell, or other backdoors of eval type',
                confidence: 90
            }            
        }
    }

    return clean
})


plugin.register('readFile', function (params, context) {
    var server = context.server

    //
    //[Recent adjustment]
    // Algorithm 1: Compare with the URL to check if it is a successful directory scan. Only for java webdav mode
    // 
    // Note: This method is limited by readfile.extension.regex and resource file size
    // https://rasp.baidu.com/doc/setup/others.html#java-common
    // 
    if (1 && server.language == 'java') {
        var filename_1 = basename(context.url)
        var filename_2 = basename(params.realpath)

        if (filename_1 == filename_2) {
            var matched = false

            // Try to download compressed packages, SQL files, etc.
            if (forcefulBrowsing.dotFiles.test(filename_1)) {
                matched = true
            } else {
                // Try to access sensitive files
                for (var i = 0; i < forcefulBrowsing.unwantedFilenames; i ++) {
                    if (forcefulBrowsing.unwantedFilenames[i] == filename_1) {
                        matched = true
                    }
                }
            }

            if (matched) {
                return {
                    action:     'log',
                    message: 'Try to download sensitive files (' + context.method.toUpperCase() + ' way): ' + params.realpath,

                    // If the HEAD method downloads sensitive files, 100% scanner attack
                    confidence: context.method == 'head' ? 100 : 90
                }
            }
        }
    }

    //
    // Algorithm 2: File, directory probe
    // If the application reads a file from the list, such as /root/.bash_history, this usually means backdoor operation
    // 
    if (algorithmConfig.readFile_unwanted.action != 'ignore')
    {
        var realpath_lc = params.realpath.toLowerCase()

        for (var j = 0; j < forcefulBrowsing.absolutePaths.length; j ++) {
            if (forcefulBrowsing.absolutePaths[j] == realpath_lc) {
                return {
                    action:     algorithmConfig.readFile_unwanted.action,
                    message: 'WebShell/File Manager - Try to read the system file: ' + params.realpath,
                    confidence: 90
                }
            }
        }
    }

    //
    // Algorithm 3: Check the file traversal to see if it is beyond the scope of the web directory
    // eg Use ../../../etc/passwd to read files across directories
    // 
    if (algorithmConfig.readFile_traversal.action != 'ignore') 
    {
        var path        = params.path
        var appBasePath = context.appBasePath

        if (is_outside_webroot(appBasePath, params.realpath, path)) {
            return {
                action:     algorithmConfig.readFile_traversal.action,
                message: 'Directory traversal attack, jumping out of the web directory range (' + appBasePath + ')',
                confidence: 90
            }
        }
    }

    //
    // Algorithm 4: Intercept any file download vulnerability, the file to be read comes from user input, and there is no path stitching
    //
    // does not affect the normal operation, eg
    // ?path=download/1.jpg
    // 
    if (algorithmConfig.readFile_userinput.action != 'ignore')
    {
        if (is_from_userinput(context.parameter, params.path))
        {
            var path_lc = params.path.toLowerCase()

            // 1. Use absolute path
            // ?file=/etc/./hosts
            if (is_absolute_path(params.path, context.server.os))
            {
                return {
                    action:     algorithmConfig.readFile_userinput.action,
                    message: 'Any file download attack (absolute path), target file: ' + params.realpath,
                    confidence: 90
                }   
            }

            // 2. Relative path and include /../
            // ?file=download/../../etc/passwd
            if (hasTraversal(params.path))
            {
                return {
                    action:     algorithmConfig.readFile_userinput.action,
                    message: 'Any file download attack (relative path), target file: ' + params.realpath,
                    confidence: 90
                }                   
            }

            // 3. Read http(s):// content
            // ?file=http://www.baidu.com
            if (path_lc.startsWith('http://') || path_lc.startsWith('https://'))
            {
                if (algorithmConfig.readFile_userinput_http.action != 'ignore')
                {
                    return {
                        action:     algorithmConfig.readFile_userinput_http.action,
                        message: 'any file read, target URL: ' + params.path,
                        confidence: 90
                    }
                }
            }

            // 4. Read file:// content
            // ?file=file:///etc/passwd
            if (path_lc.startsWith('file://'))
            {
                if (algorithmConfig.readFile_userinput_file.action != 'ignore')
                {
                    return {
                        action:     algorithmConfig.readFile_userinput_file.action,
                        message: 'any file read, target file: ' + params.path,
                        confidence: 90
                    } 
                }
            }            
        }
    }

    return clean
})

plugin.register('include', function (params, context) {
    var url = params.url

    // If there is no agreement
    // ?file=../../../../../var/log/httpd/error.log
    if (url.indexOf('://') == -1) {
        var realpath    = params.realpath
        var appBasePath = context.appBasePath

        // Is it jumping out of the web directory?
        if (algorithmConfig.include_outsideWebroot.action != 'ignore' &&
            is_outside_webroot(appBasePath, realpath, url)) 
        {
            return {
                action:     algorithmConfig.include_outsideWebroot.action,
                message: 'Any file contains an attack containing a file outside the scope of the web directory (' + appBasePath + ')',
                confidence: 100
            }
        }

        return clean
    }

    // If there is an agreement
    // include ('http://xxxxx')
    var items = url.split('://')

    // http mode SSRF/RFI
    if (items[0].toLowerCase() == 'http') 
    {
        if (algorithmConfig.include_http.action != 'ignore')
        {
            return {
                action:     algorithmConfig.include_http.action,
                message: 'SSRF vulnerability: ' + params.function + ' mode',
                confidence: 70
            }  
        }        
    }

    // file protocol
    if (items[0].toLowerCase() == 'file') {
        var basename = items[1].split('/').pop()

        // Is it a directory?
        if (items[1].endsWith('/')) {
            // Some applications, if you directly include the directory, will list the contents of this directory
            if (algorithmConfig.include_dir.action != 'ignore') {
                return {
                    action:     algorithmConfig.include_dir.action,
                    message: 'sensitive directory access: ' + params.function + ' mode',
                    confidence: 100
                }
            }
        }

        // Is it a sensitive file?
        if (algorithmConfig.include_unwanted.action != 'ignore') {
            for (var i = 0; i < forcefulBrowsing.unwantedFilenames.length; i ++) {
                if (basename == forcefulBrowsing.unwantedFilenames[i]) {
                    return {
                        action:     algorithmConfig.include_unwanted.action,
                        message: 'sensitive file download: ' + params.function + ' way',
                        confidence: 100
                    }
                }
            }
        }
    }

    return clean
})


plugin.register('writeFile', function (params, context) {

    // Write NTFS stream file, certainly not normal
    if (algorithmConfig.writeFile_NTFS.action != 'ignore') 
    {
        if (ntfsRegex.test(params.realpath)) {
            return {
                action:     algorithmConfig.writeFile_NTFS.action,
                message: 'Try to upload the backdoor with NTFS stream: ' + params.realpath,
                confidence: 90
            }
        }
    }

    // PUT upload
    if (context.method == 'put' &&
        algorithmConfig.writeFile_PUT_script.action != 'ignore') 
    {
        if (scriptFileRegex.test(params.realpath)) {
            return {
                action:     algorithmConfig.writeFile_PUT_script.action,
                message: 'Upload the script file using PUT, path: ' + params.realpath,
                confidence: 90
            }
        }        
    }

    // For this algorithm, please refer to this plugin custom documentation
    // https://rasp.baidu.com/doc/dev/official.html#case-3    
    if (algorithmConfig.writeFile_script.action != 'ignore') 
    {
        if (scriptFileRegex.test(params.realpath)) {
            return {
                action:     algorithmConfig.writeFile_script.action,
                message: 'Try writing to the script file, path: ' + params.realpath,
                confidence: 90
            }
        }
    }
    return clean
})


if (algorithmConfig.fileUpload_multipart.action != 'ignore') 
{
    plugin.register('fileUpload', function (params, context) {

        if (scriptFileRegex.test(params.filename) || ntfsRegex.test(params.filename)) {
            return {
                action:     algorithmConfig.fileUpload_multipart.action,
                message: 'Try uploading the script file: ' + params.filename,
                confidence: 90
            }
        }

        if (params.filename == ".htaccess" || params.filename == ".user.ini") {
            return {
                action:     algorithmConfig.fileUpload_multipart.action,
                message: 'Try uploading the Apache/PHP configuration file: ' + params.filename,
                confidence: 90
            } 
        }

        return clean
    })
}


if (algorithmConfig.fileUpload_webdav.action != 'ignore')
{
    plugin.register('webdav', function (params, context) {
        
        // The source file is not a script &amp;&amp; The target file is a script, and it is determined that the MOVE mode is written to the back door.
        if (! scriptFileRegex.test(params.source) && scriptFileRegex.test(params.dest)) 
        {
            return {
                action:    algorithmConfig.fileUpload_webdav.action,
                message: 'Try uploading the script file via ' + context.method + ': ' + params.dest,
                confidence: 100
            }
        }

        return clean
    })
}

if (algorithmConfig.rename_webshell.action != 'ignore')
{
    plugin.register('rename', function (params, context) {
        
        // The source file is not a script, and the target file is a script, and it is determined that the rename method is to write the back door.
        if (! scriptFileRegex.test(params.source) && scriptFileRegex.test(params.dest)) 
        {
            return {
                action:    algorithmConfig.rename_webshell.action,
                message: 'Rename the way to get the webshell, source file: ' + params.source,
                confidence: 100
            }
        }

        return clean
    })
}


plugin.register('command', function (params, context) {
    var server  = context.server
    var message = undefined

    // Algorithm 1: Check for a deserialization attack based on the stack.
    // Theoretically, there is no false positive for this algorithm.

    if (algorithmConfig.command_reflect.action != 'ignore') {
        // Java detection logic
        if (server.language == 'java') {
            var userCode = false
            var known    = {
                'java.lang.reflect.Method.invoke': 'Try to execute the command by reflection',
                'ognl.OgnlRuntime.invokeMethod': 'Try to execute the command via OGNL code',
                'com.thoughtworks.xstream.XStream.unmarshal': 'Try to execute the command via xstream deserialization',
                'org.apache.commons.collections4.functors.InvokerTransformer.transform': 'Try to execute the command through the transformer deserialization',
                'org.jolokia.jsr160.Jsr160RequestDispatcher.dispatchRequest': 'Try to execute the command via JNDI injection method',
                'com.alibaba.fastjson.parser.deserializer.JavaBeanDeserializer.deserialze': 'Try to execute the command via fastjson deserialization',
                'org.springframework.expression.spel.support.ReflectiveMethodExecutor.execute': 'Try to execute commands via Spring SpEL expressions',
                'freemarker.template.utility.Execute.exec': 'Try to execute commands via FreeMarker template'
            }
            
            for (var i = 2; i < params.stack.length; i ++) {
                var method = params.stack[i]

                if (method.startsWith('ysoserial.Pwner')) {
                    message = 'YsoSerial exploit tool - deserialization attack'
                    break
                }

                if (method == 'org.codehaus.groovy.runtime.ProcessGroovyMethods.execute') {
                    message = 'Try to execute the command via Groovy script'
                    break
                }

                // Intercept only if the command itself is from a reflection call
                // If a class is a reflection call, this class then actively execute the command, then ignore
                if (! method.startsWith('java.') && ! method.startsWith('sun.') && !method.startsWith('com.sun.')) {
                    userCode = true
                }

                if (known[method]) {
                    // Same as above, if the user code is included between the reflection call and the command execution, it is not considered to be a reflection call.
                    if (userCode && method == 'java.lang.reflect.Method.invoke') {
                        continue
                    }

                    message = known[method]
                    // break
                }
            }
        }

        // PHP detection logic
        else if (server.language == 'php' && validate_stack_php(params.stack)) 
        {
            message = 'Discover Webshell, or a code execution vulnerability based on type eval/assert/create_function/preg_replace/..'
        }

        if (message) 
        {
            return {
                action:     algorithmConfig.command_reflect.action,
                message:    message,
                confidence: 100
            }
        }
    }

    // Algorithm 2: Command execution is disabled by default
    // Change to log or ignore if needed
    // Or according to the URL to decide whether to allow the execution of the command

    // Starting with v0.31, when the command is executed from a non-HTTP request, we will also detect the deserialization attack.
    // But should not intercept normal command execution, so add a context.url check here
    if (! context.url) {
        return clean
    }

    if (algorithmConfig.command_other.action == 'ignore') {
        return clean
    } else {
        return {
            action:     algorithmConfig.command_other.action,
            message: 'Try to execute the command',
            confidence: 90
        } 
    }

})


// Note: Since libxml2 cannot be hooked, PHP does not support XXE detection for the time being.
plugin.register('xxe', function (params, context) {
    var items = params.entity.split('://')

    if (items.length >= 2) {
        var protocol = items[0]
        var address  = items[1]

        // reject special agreement
        if (algorithmConfig.xxe_protocol.action != 'ignore') {
            if (protocol === 'gopher' || protocol === 'ftp' || protocol === 'dict' || protocol === 'expect') {
                return {
                    action:     algorithmConfig.xxe_protocol.action,
                    message: 'SSRF/Blind XXE attack (' + protocol + 'protocol)',
                    confidence: 100
                }
            }
        }

        // file protocol + absolute path, eg
        // file:///etc/passwd
        //
        // Relative path is prone to false positives, eg
        // file://xwork.dtd
        if (address.length > 0 && protocol === 'file' && address[0] == '/') {
            return {
                action:     'log',
                message: 'Try to read external entity (file protocol)',
                confidence: 90
            }
        }
    }
    return clean
})

if (algorithmConfig.ognl_exec.action != 'ignore') 
{
    // By default, when the OGNL expression is longer than 30, it will enter the detection point. This length can be configured.
    plugin.register('ognl', function (params, context) {
        // Common struts payload statement features
        var ognlPayloads = [
            'ognl.OgnlContext',
            'ognl.TypeConverter',
            'ognl.MemberAccess',
            '_memberAccess',
            'ognl.ClassResolver',
            'java.lang.Runtime',
            'java.lang.Class',
            'java.lang.ClassLoader',
            'java.lang.System',
            'java.lang.ProcessBuilder',
            'java.lang.Object', 
            'java.lang.Shutdown',
            'java.io.File',
            'javax.script.ScriptEngineManager',
            'com.opensymphony.xwork2.ActionContext'
        ]

        var ognlExpression = params.expression
        for (var index in ognlPayloads) 
        {
            if (ognlExpression.indexOf(ognlPayloads[index]) > -1) 
            {
                return {
                    action:     algorithmConfig.ognl_exec.action,
                    message: 'Try ognl remote command execution',
                    confidence: 100
                }
            }

        }
        return clean
    })
}


// [[ Recent adjustments ~ ]]
if (algorithmConfig.transformer_deser.action != 'ignore') {
    plugin.register('deserialization', function (params, context) {
        var deserializationInvalidClazz = [
            'org.apache.commons.collections.functors.InvokerTransformer',
            'org.apache.commons.collections.functors.InstantiateTransformer',
            'org.apache.commons.collections4.functors.InvokerTransformer',
            'org.apache.commons.collections4.functors.InstantiateTransformer',
            'org.codehaus.groovy.runtime.ConvertedClosure',
            'org.codehaus.groovy.runtime.MethodClosure',
            'org.springframework.beans.factory.ObjectFactory',
            'xalan.internal.xsltc.trax.TemplatesImpl'
        ]

        var clazz = params.clazz
        for (var index in deserializationInvalidClazz) {
            if (clazz === deserializationInvalidClazz[index]) {
                return {
                    action:     algorithmConfig.transformer_deser.action,
                    message: 'Try to deserialize the attack',
                    confidence: 100
                }
            }
        }
        return clean
    })
}

plugin.log ('Official plugin: Initialization successful')
