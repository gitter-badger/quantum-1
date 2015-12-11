quantum
==========
[![Stack Share](http://img.shields.io/badge/tech-stack-0690fa.svg?style=flat)](http://stackshare.io/alexandergunnarson/clojure-clojurescript-datomic)

![](http://pre03.deviantart.net/b712/th/pre/i/2012/267/e/3/bubble_chamber_by_deepbluerenegade-d5fssqg.jpg)
*(Image credit: @deviantart/deepbluerenegade, "Bubble Chamber")*

***"The career of a young theoretical physicist consists of treating the harmonic oscillator in ever-increasing levels of abstraction."*** *— Sidney Coleman*

-

In like manner, the aim of a computer scientist consists of treating the manipulation and processing of data (including procedures) in ever-increasing levels of abstraction.

In accordance with this aim, to paraphrase the [poet of xkcd](http://xkcd.com/224/):

***That syntax might fade,***

***That all might swim in the purity***

***Of quantified conception, of ideas manifest,***

***To this end, from the ancience of Lisp —***

***Time-tested, but never time-worn —***

***Was fashioned***

`quantum`.

Summary
-
Less magically put, quantum is an all-purpose Clojure library similar to [Prismatic/plumbing](https://github.com/Prismatic/plumbing), [weavejester/medley](https://github.com/weavejester/medley), [mikera/clojure-utils](https://github.com/mikera/clojure-utils), [ptaoussanis/encore](https://github.com/ptaoussanis/encore), [ztellman/potemkin](https://github.com/ztellman/potemkin), and others. It aims to unify and abstract away conceptually irrelevant implementation details while providing great performance, often exceeding that of clojure.core (see benchmarks for more information).

It adapts, in the author's opinion, the best and most useful functions from existing libraries (in accordance with their respective copyrights) and adds much more of its own.

Use
-

**In `project.clj`:**

![](https://clojars.org/quantum/core/latest-version.svg)

**In namespaces:**

`(require 'quantum.core. ... )`
where `...` is the name of the sub-library (e.g., `quantum.core.collections`, `quantum.core.io`, etc.).

Performance
-

#####Reducers
It uses clojure.reducers wherever possible to maximize performance, but falls back on clojure.core collections functions when laziness is required or desired, or when the overhead of creating an anonymous function within `reduce` is greater than the overhead eliminated by using `reduce` instead of [`first` and [`next` or `rest`] within `loop`/`recur`].

#####Transients and Mutable Locals
It uses transients and/or mutable local variables wherever a performance boost can be achieved by such.

Expectations
-

This is a work in progress and, as such, is currently in no way as rich and polished a library as I hope it to ultimately be. When you find bugs (and you will), please report the issue(s) and/or create a pull request.

I've been coding in Clojure for over two years, but expect some rustiness. Expect some gems as well.

I welcome any and all contributions, comments, thoughts, suggestions, and/or feedback you may wish to provide. This endeavor towards greater efficiency in thought, processing power, and time spent programming should be a joint one.

Copyright and License
-
*Copyright © 2014 Alex Gunnarson*

*Distributed under the Creative Commons Attribution-ShareAlike 3.0 US (CC-SA) license.*

**For normal people who don't speak legalese, this means:**

* You **can** modify the code
* You **can** distribute the code
* You **can** use the code for commercial purposes

But:

* You **have to** give credit / attribute the code to the author (Alex Gunnarson)
* You **have to** state the name of the author (Alex Gunnarson) and the title of this project in the attribution
* You **have to** say in the attribution that you modified the code if you did

Pretty easy, common-sense, decent stuff! Thanks :)

*For more information, see [tldrlegal's summary](https://tldrlegal.com/license/creative-commons-attribution-share-alike-(cc-sa)) of the CC-SA license.*
