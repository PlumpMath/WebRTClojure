(ns webrtclojure.handler
  (:require [compojure.core            :refer [GET POST defroutes]]
            [compojure.route           :refer [not-found resources]]
            [webrtclojure.middleware   :refer [wrap-middleware]]
            [webrtclojure.templates    :refer [loading-page]]
            [webrtclojure.broadcasting :refer [start-broadcaster! broadcast!]]
            [config.core               :refer [env]]
            [taoensso.sente            :as sente]
            [taoensso.sente.server-adapters.http-kit
                                       :refer (sente-web-server-adapter)]))


(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter {})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def receive-channel               ch-recv) ; ChannelSocket's receive channel
  (def channel-send!                 send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

;;; -------------------------
;;; Routes

(defmulti -message-handler "Entry point for messages over sente." :id)

;;; Non-application specific routes

(defn message-handler
  "Wraps `-message-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-message-handler ev-msg))

(defmethod -message-handler :default ; Unhandled message.
  [{:as ev-msg :keys [event]}]
  (println "Unhandled event: %s" event))

(defmethod -message-handler :chsk/ws-ping [something] ; Ping from clients
  (println "We got a ws-ping")
  )

;;; Application specific routes
(defmethod -message-handler :webrtclojure/signal
  [{:as ev-msg :keys [?data]}]
  (println "Server received a broadcast: %s" :ev-msg)
  (println ev-msg)
  (broadcast! :ev-msg connected-uids channel-send!))

;;; -------------------------
;;; Router lifecycle.

(defonce router (atom nil))
;; Stop router if it exists.
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router
          (sente/start-chsk-router! receive-channel message-handler)))

(defonce is-router-started? (start-router!))


(defroutes routes
  (GET "/" [] loading-page)
  (GET "/about" [] loading-page)
  (GET  "/sente" req (ring-ajax-get-or-ws-handshake req))
  (POST "/sente" req (ring-ajax-post                req))

  (GET "/broadcast" [] (broadcast! 9001 connected-uids channel-send!)(broadcast! 9001 :sente/all-users-without-uid channel-send!))

  (GET "/restart-sente-router" [] (start-router!))

  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
