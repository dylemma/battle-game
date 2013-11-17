package io.dylemma.util

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.async.Async.{ async, await }

object AsyncHelpers {
	implicit class ListFutureFold[T](list: List[T]) {
		def futureFoldLeft[Z](init: Z)(fold: (Z, T) => Future[Z])(implicit exc: ExecutionContext): Future[Z] = {
			def recurse(accum: Z, remain: List[T]): Future[Z] = async {
				remain match {
					case Nil => accum
					case head :: tail =>
						val accum2 = await(fold(accum, head))
						await(recurse(accum2, tail))
				}
			}
			recurse(init, list)
		}
	}
}