// namespaced:
//
// Usage: namespaced('some', 'path', 'whatever', function(scope){...})
// The first N arguments are namespace segments. For example, with 3
// segment arguments ('some', 'path', 'whatever'), this function will
// make sure that `this.some.path.whatever` exists, and pass it to
// the last argument.
function namespaced(){
	var ns = this
	while(arguments.length > 1){
		var segment = Array.prototype.shift.call(arguments)
		var child = ns[segment] || (ns[segment] = {})
		ns = child
	}
	var setup = arguments[0]
	setup(ns)
}