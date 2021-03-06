(ns webrtclojure.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary]
              [accountant.core :as accountant]
              [taoensso.sente  :as sente]
              [ajax.core :refer [GET POST]] ; Only for testing
              [webrtclojure.server-comms :as server-comms]
              [webrtclojure.webrtc :as webrtc]
              [webrtclojure.pages :as pages]))

;;; -------------------------
;;; State

(defn current-page []
  [(session/get :current-page)])

;;; -------------------------
;;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'pages/home))

(secretary/defroute "/about" []
  (session/put! :current-page #'pages/about))

(secretary/defroute "/register" []
  (session/put! :current-page #'pages/registry))

(secretary/defroute "/chat" []
  (session/put! :current-page #'pages/chat))

;;; -------------------------
;;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))

(defonce is-router-started? (server-comms/start-router!))
