package io.dylemma.server

import spray.http.HttpCookie
import spray.http.HttpHeader
import spray.http.HttpHeaders.Cookie

object HeaderExtractors {

	/** Extractor that duplicates the input, allowing for multiple
	  * extractors to be run on a single input in a match/case. E.g.
	  *
	  * {{{thing match {
	  * case t @ SomeExtractor(a) @~ AnotherExtractor(x) => ...
	  * } }}}
	  */
	object @~ {
		def unapply[T](any: T) = Some(any -> any)
	}

	object HasSessionCookie {
		def unapply(headers: List[HttpHeader]): Option[SessionId] = {
			val itr = for {
				Cookie(c) <- headers.iterator
				SessionId.Cookie(s) <- c
			} yield s

			if (itr.hasNext) Some(itr.next) else None
		}
	}
}