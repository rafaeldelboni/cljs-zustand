(ns cc.delboni.cljs-zustand.app
  (:require ["react-dom/client" :as rdom]
            ["zustand" :as zustand]
            [cc.delboni.cljs-zustand.infra.helix :refer [defnc]]
            [helix.core :refer [$]]
            [helix.dom :as d]))

(defn sleep [ms]
  (js/Promise.
   (fn [res _rej]
     (js/setTimeout
      (fn [] (res))
      ms))))

(def use-counter-store
  (zustand/create
   (fn [set]
     (clj->js
      {:counter 0
       :increaseFn #(set (fn [state]
                           (clj->js {:counter (-> state .-counter inc)})))}))))

(def use-counter-async-store
  (zustand/create
   (fn [set]
     #js {:counter 0
          :loading false
          :error nil
          :increaseFn (fn []
                        (set (fn [_] #js {:loading true}))
                        (-> (sleep 1000)
                            (.then (fn [_res]
                                     (set (fn [state]
                                            #js {:loading false
                                                 :error nil
                                                 :counter (-> state .-counter inc)}))))
                            (.catch (fn [err]
                                      (js/console.error err)
                                      (set (fn [_]
                                             #js {:loading false
                                                  :error err}))))))})))

;; app
(defnc app []
  (let [counter (use-counter-store (fn [state] (js->clj state :keywordize-keys true)))
        counter-async (use-counter-async-store (fn [state] (js->clj state :keywordize-keys true)))]
    (d/div
      (d/h1 "cljs-zustand")
      (d/div
        (d/h3 (str "Counter: " (:counter counter)))
        (d/button {:on-click #((:increaseFn counter))} "Count"))
      (d/div
        (d/h3 (str "Counter Async: " (:counter counter-async)))
        (d/button {:disabled (:loading counter-async) :on-click #((:increaseFn counter-async))} "Count")))))

;; start your app with your React renderer
(defn ^:export init []
  (doto (rdom/createRoot (js/document.getElementById "app"))
    (.render ($ app))))
