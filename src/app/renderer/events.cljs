(ns app.renderer.events
  (:require [re-frame.core :as rf]
            [com.rpl.specter :as sp]))

(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:templates []
    :characters {}
    :factions {}
    :selections {:tab :character-select
                 :current-character nil
                 :highlighted {:character nil
                               :ability nil}}
    :initiative (sorted-map)
    :settings nil}))

(defn index-by-key [key seq]
  (#(zipmap (map key %) %) seq))

(rf/reg-event-db
 :initialize-templates
 (fn [db [_ templates]]
   (let [template-types (group-by :type templates)
         characters (get template-types "character" [])
         factions (->> (get template-types "factions" {})
                     (mapcat :factions)
                     (index-by-key :id))]
     (-> db
        (assoc :templates characters)
        (assoc :factions factions)))))

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
   (assoc-in db [:selections :current-character] character-uuid)))

(rf/reg-event-db
 :use-ability
 (fn [db [_ character-uuid {:keys [id cooldown ap duration]}]]
   (let [character (get-in db [:characters character-uuid])
         ability (-> (get-in character [:abilities id])
                    (assoc-in [:tracker/internal :back-in] cooldown)
                    (cond-> duration (assoc-in [:tracker/internal :duration-left] duration)))
         character (-> character
                      (assoc-in [:tracker/internal :ap-left]
                                (- (or (:ap-left (:tracker/internal character))
                                       (:ap character))
                                   ap))
                      (assoc-in [:abilities id] ability))]
     (assoc-in db [:characters character-uuid] character))))

(rf/reg-event-db
 :increment-round
 (fn [db _]
   (->> db
      (sp/transform [:characters sp/MAP-VALS :abilities sp/MAP-VALS :tracker/internal :back-in pos?] dec)
      (sp/transform [:characters sp/MAP-VALS :abilities sp/MAP-VALS :tracker/internal :duration-left pos?] dec)
      (sp/transform [:characters sp/MAP-VALS] #(assoc-in % [:tracker/internal :ap-left] (:ap %))))))

(rf/reg-event-db
 :increment-interleaved-round
 (fn [db _]
   (->> db
      (sp/transform [:characters sp/MAP-VALS #(:interleaved? %) :abilities sp/MAP-VALS :tracker/internal :back-in pos?] dec)
      (sp/transform [:characters sp/MAP-VALS #(:interleaved? %) :abilities sp/MAP-VALS :tracker/internal :duration-left pos?] dec)
      (sp/transform [:characters sp/MAP-VALS #(:interleaved? %)] #(assoc-in % [:tracker/internal :ap-left] (:ap %))))))

(rf/reg-event-db
 :set-highlighted-ability
 (fn [db [_ character-id ability-id]]
   (assoc-in db [:selections :highlighted] {:character character-id
                                            :ability ability-id})))

(rf/reg-event-db
 :remove-highlighted-ability
 (fn [db _]
   (assoc-in db [:selections :highlighted] {:character nil
                                            :ability nil})))

(rf/reg-event-db
 :change-tab
 (fn [db [_ tab]]
   (assoc-in db [:selections :tab] tab)))

(rf/reg-event-db
 :inc-ap-left
 (fn [db [_ char-uuid]]
   (update-in db [:characters char-uuid :tracker/internal :ap-left] inc)))

(rf/reg-event-db
 :dec-ap-left
 (fn [db [_ char-uuid]]
   (update-in db [:characters char-uuid :tracker/internal :ap-left] dec)))

(rf/reg-event-db
 :inc-health-left
 (fn [db [_ char-uuid]]
   (update-in db [:characters char-uuid :tracker/internal :health-left] inc)))

(rf/reg-event-db
 :dec-health-left
 (fn [db [_ char-uuid]]
   (update-in db [:characters char-uuid :tracker/internal :health-left] dec)))

;; ** Settings
(rf/reg-event-db
 :set-settings
 (fn [db [_ settings]]
   (assoc db :settings settings)))
