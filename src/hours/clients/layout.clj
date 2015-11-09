(ns hours.clients.layout)

(defn display-add-client []
  [:form {:method "POST" :action "/client/add"}
   [:div.input-group
    [:input.form-control {:type "text" :name "name" :placeholder "Client name..."}]
     [:span.input-group-btn
      [:button.btn.btn-default {:type "submit"} "Add"]]]])

(defn display-clients [clients]
  (list [:table.table
         [:tbody
          [:tr
           [:th "Name"]
           [:th "&nbsp;"]]
          (for [client clients]
            [:tr
             [:td (:name client)]
             [:td [:a {:href (str "/project/add/" (:id client))} "Add project"]
              " | "
              [:a {:href (str "/client/" (:id client) "/projects")} "Show projects"]]])]]
        [:div [:a {:href "/client/add"} "Add client"]]))
