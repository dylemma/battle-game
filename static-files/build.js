var browserify = require('browserify'),
	browserifyShim = require('browserify-shim'),
	coffeeify = require('coffeeify'),
	fs = require('fs'),
	mkdirp = require('mkdirp'),
	path = require('path')

var distPath = path.join(__dirname, 'dist')

function buildify(inputPath){
	// Input path: '/X/Y/Z/file.whatever'
	// Output path: 'dist/X/Y/Z/file_bundled.js'
	var ext = path.extname(inputPath),
		inputMinusExt = inputPath.slice(0, -ext.length),
		bundlePath = path.join(distPath, inputMinusExt + '_bundled.js'),
		parent = path.dirname(bundlePath)

	mkdirp.sync(parent)

	browserify({ extensions: ['.coffee'] })
		.require(inputPath, { entry: true })
		.transform(coffeeify)
		.transform(browserifyShim)
		.bundle({ debug: true })
		.pipe(fs.createWriteStream(bundlePath, 'utf-8'))
		.on('finish', function(){ console.log('Wrote ' + bundlePath) })
}

buildify('./Main.coffee')
buildify('./index.coffee')
buildify('./hexgrid.coffee')