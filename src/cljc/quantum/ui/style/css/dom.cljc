(ns quantum.ui.style.css.dom
           (:refer-clojure :exclude [assert])
           (:require [clojure.string     :as str]
                     [quantum.core.error :as err
                       :refer [#?(:clj assert)] ]
                     [quantum.core.fn    :as fn
                       :refer [#?@(:clj [fn->])]]
                     [quantum.core.logic :as logic
                       :refer [#?@(:clj [fn-and]) nnil?]])
  #?(:cljs (:require-macros
                     [quantum.core.logic :as logic
                       :refer [fn-and]            ]
                     [quantum.core.error :as err
                       :refer [assert]            ]
                     [quantum.core.fn    :as fn
                       :refer [fn->]              ])))

#?(:cljs
(defn add-link! [link]
  (let [elem (.createElement js/document "link")]
    (set! (.-href elem) link)
    (set! (.-rel  elem) "stylesheet")
    (set! (.-type elem) "text/css")
    (.appendChild (.-head js/document) elem)
    elem)))

#?(:cljs
(defn append-css! [css-str]
  "Inserts stylesheet into document head"
  {:from "https://github.com/facjure/gardener/dom"}
  (let [elem (.createElement  js/document "style")
        text (.createTextNode js/document css-str)]
    (.appendChild elem text)
    (.appendChild (.-head js/document) elem)
    elem)))

#?(:cljs
(defn replace-css-at! [id css-str]
  "Replaces CSS at a style node."
  (let [elem (.getElementById js/document id)
        _ (assert ((fn-and nnil? (fn-> .-tagName str/lower-case (= "style"))) elem) #{elem})
        text (.createTextNode js/document css-str)]
    (while (.-firstChild elem)
      (.removeChild elem (.-firstChild elem)))

    (.appendChild elem text)
    elem)))

; Comment this out to not create CSS dynamically
;(append-css! (css-map->css-string css))

; Uncomment this out to pre-create CSS file
;#?(:clj (spit "dev-resources/public/todos.css" @css-string))


; TODO move?

#?(:cljs
(defn viewport-w []
  (-> js/document .-documentElement .-clientWidth)))

#?(:cljs
(defn viewport-h []
  (-> js/document .-documentElement .-clientHeight)))

#?(:cljs
(defn visual-length [text]
  (let [elem (.createElement js/document "span")]
    (set! (.-innerHTML elem) text)
    (.-clientWidth elem))))