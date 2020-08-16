(ns app.renderer.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:templates []
    :characters {}
    :selected-character-id nil
    :tab :character-select
    :highlighted-ability {:character-id nil
                          :ability-id nil}}))

(rf/reg-event-db
 :initialize-templates
 (fn [db [_ templates]]
   (assoc db :templates templates)))

(rf/reg-event-db
 :add-character
 (fn [db [_ character]]
   (update db :characters assoc (:uuid character) character)))

(rf/reg-event-db
 :remove-character
 (fn [db [_ character-uuid]]
   (update db :characters dissoc character-uuid)))

(rf/reg-event-db
 :set-selected-character-id
 (fn [db [_ character-uuid]]
   (assoc db :selected-character-id character-uuid)))

(defn spy [x] (cljs.pprint/pprint x) x)

(rf/reg-event-db
 :use-ability
 (fn [db [_ character-uuid ability]]
   (assoc-in db
              [:characters character-uuid :abilities (:id ability) :back-in]
              (:cooldown ability))))

(rf/reg-event-db
 :increment-round
 (fn [db _]
   (let [paths (into []
                     (for [{:keys [uuid abilities]} (-> db :characters vals)
                           [id ability] abilities
                           :when (pos? (get ability :back-in))]
                       [uuid id]))
         characters (:characters db)
         reducer (fn [m idx [char-id ability-id]]
                   (update-in m [char-id :abilities ability-id :back-in] dec))]
     (assoc db :characters (reduce-kv reducer characters paths)))))

(rf/reg-event-db
 :increment-interleaved-round
 (fn [db _]
   (let [paths (into []
                     (for [{:keys [uuid interleaved? abilities]} (-> db :characters vals)
                           [id ability] abilities
                           :when (and (pos? (get ability :back-in))
                                      interleaved?)]
                       [uuid id]))
         characters (:characters db)
         reducer (fn [m idx [char-id ability-id]]
                   (update-in m [char-id :abilities ability-id :back-in] dec))]
     (assoc db :characters (reduce-kv reducer characters paths)))))

(rf/reg-event-db
 :set-highlighted-ability
 (fn [db [_ character-id ability-id]]
   (assoc db :highlighted-ability {:character-id character-id
                                   :ability-id ability-id})))

(rf/reg-event-db
 :remove-highlighted-ability
 (fn [db _]
   (assoc db :highlighted-ability {:character-id nil
                                   :ability-id nil})))

(rf/reg-event-db
 :change-tab
 (fn [db [_ tab]]
   (assoc db :tab tab)))
