var plugin = new RASP('offical')
var clean  = {
  action: 'ignore',
  message: 'no risk',
  confidence: 0
}

plugin.register('request', function(params, context) {
  var header = context.header
  var method = context.method
  var reason = false

  if (! header['accept']) {
    reason = 'missing Accept request header'
  } else if (method != 'get' && method != 'post' && method != 'head') {
    reason = method.toUpperCase()
  }

  if (reason) {
    return {
      action:     'block',
      message: 'Unusual request method: ' + reason,
      confidence: 90
    }
  }

  return clean
})

plugin.log('003-unusual-request load completed')

