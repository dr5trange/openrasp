//
// OpenRASP event logging plugin - DEMO
// 

'use strict'
var plugin  = new RASP('event-logger')

const clean = {
    action:     'ignore',
    message: 'no risk',
    confidence: 0
}

plugin.register('directory', function (params, context) {
    plugin.log('List of directories: ' + params.realpath)
    return clean
})

plugin.register('webdav', function (params, context) {
    plugin.log ('Use WEBDAV operation file: ', context.method, params.source, params.dest)
    return clean
})

plugin.register('fileUpload', function (params, context) {
    plugin.log('file upload: ' + params.filename)
    return clean
})

plugin.register('command', function (params, context) {
    plugin.log('command execution: ' + params.command)
    return clean
})

// In order to improve performance, the plugin will only be called when the OGNL expression is longer than 30.
// This 30 can be configured, aka "ognl.expression.minlength"
// https://rasp.baidu.com/doc/setup/others.html
plugin.register('ognl', function (params, context) {
    plugin.log('Execute OGNL expression: ' + params.expression)
    return clean
})

// The following methods may generate a lot of logs
plugin.register('xxe', function (params, context) {
    plugin.log('Read XML external entity: ' + params.entity)
    return clean
})

plugin.register('include', function (params, context) {
    plugin.log('file contains: ' + params.url)
    return clean
})

plugin.register('readFile', function (params, context) {
    plugin.log('read file: ' + params.realpath)
    return clean
})

plugin.register('writeFile', function (params, context) {
    plugin.log('file write: ' + params.realpath)
    return clean
})

function normalize_query(query) {
    var tokens = RASP.sql_tokenize(query)
    for (var i = 0; i < tokens.length; i ++) {
        var token = tokens[i]

        // check if it is a string
        if ( (token[0] == "'" || token[0] == '"') &&
            (token[token.length - 1] == "'" || token[token.length - 1] == '"'))
        {
            tokens[i] = '"S"'
        }
    }

    return tokens.join(' ')
}

// Record the SQL log, may bring the following two problems
// 1. Sensitive information may be included in the query
// 2. The amount of logs can be very large
plugin.register('sql', function (params, context) {
    plugin.log('SQLInquire : ' + normalize_query(params.query))

    return clean
})

plugin.log('999-event-logger: Initialization succeeded')


