var browserify = require('browserify'),
	browserifyShim = require('browserify-shim'),
	coffeeify = require('coffeeify'),
	fs = require('fs')
	path = require('path')

var distPath = path.join(__dirname, 'dist')
fs.mkdirSync(distPath)

var bundlePath = path.join(distPath, 'bundle.js')

browserify({ extensions: ['.coffee'] })
	.require('./Main.coffee', { entry: true, expose: 'main' })
	.transform(coffeeify)
	.transform(browserifyShim)
	.bundle({ debug: true })
	.pipe(fs.createWriteStream(bundlePath, 'utf-8'))
	.on('finish', function(){ console.log('Wrote ' + bundlePath) })

var indexBundledPath = path.join(distPath, 'index_bundled.js')

browserify({ extensions: ['.coffee'] })
	.require('./index.coffee', { entry: true })
	.transform(coffeeify)
	.transform(browserifyShim)
	.bundle({ debug: true })
	.pipe(fs.createWriteStream(indexBundledPath, 'utf-8'))
	.on('finish', function(){ console.log('Wrote ' + indexBundledPath) })
	.on('error', function(e){ console.log(JSON.stringify(e)) })