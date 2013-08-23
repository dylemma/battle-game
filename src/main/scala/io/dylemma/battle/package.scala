package io.dylemma

package object battle {

	var logThreshold = All
	val All = 0
	val Trace = 1
	val Debug = 2
	val Info = 3
	val Warn = 4
	val Error = 5

	def log(level: Int, levelS: String, msgs: Any*) = if (level >= logThreshold) {
		println(s"$levelS: ${msgs mkString " "}")
	}

	def trace(msgs: Any*) = log(Trace, "trace", msgs: _*)
	def debug(msgs: Any*) = log(Debug, "debug", msgs: _*)
	def info(msgs: Any*) = log(Info, "info", msgs: _*)
	def warn(msgs: Any*) = log(Warn, "warn", msgs: _*)
	def error(msgs: Any*) = log(Error, "error", msgs: _*)

}