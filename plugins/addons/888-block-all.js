// This plugin is used to test the interception effect
// 
// The logic of this plugin is that it will be intercepted regardless of whether the request is normal or not.
// To open this plugin, first delete the following return :-)
return

'use strict'
var plugin  = new RASP('block-all-test')

const default_action = {
    action:     'block',
    message: '- plugin all intercept test-',
    confidence: 90
}

plugin.register('sql', function (params, context) {
    return default_action
})

plugin.register('ssrf', function (params, context) {
    return default_action
})

plugin.register('directory', function (params, context) {
    return default_action
})

plugin.register('readFile', function (params, context) {
    return default_action
})

plugin.register('webdav', function (params, context) {
    return default_action
})

plugin.register('include', function (params, context) {
    return default_action
})

plugin.register('writeFile', function (params, context) {
    return default_action
})

plugin.register('fileUpload', function (params, context) {
    return default_action
})

plugin.register('command', function (params, context) {
    return default_action
})

// Note: PHP does not support XXE detection
plugin.register('xxe', function (params, context) {
    return default_action
})

// By default, when the OGNL expression is longer than 30, it will enter the detection point. This length can be configured.
plugin.register('ognl', function (params, context) {
    return default_action
})

// [[ Recent adjustments ~ ]]
plugin.register('deserialization', function (params, context) {
    return default_action
})

plugin.log ('all intercept plugin test: initial success')
