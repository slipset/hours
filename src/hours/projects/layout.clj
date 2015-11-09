(ns hours.projects.layout)

(defn display-projects [projects]
  (let [client-id (:workday_client_id (first projects))]
    [:div  [:table.table
            [:tbody
             [:tr
              [:th "Name"]
              [:th "Client" ]
              [:th "&nbsp;"]]
             (for [project projects]
               [:tr
                [:td [:span {:style (str "background-color:#" (:color project))} (:name project)]]
                [:td (:name_2 project)]
                [:td [:a {:href (str "/project/edit/" (:id project))} "edit"]]])]]
     [:div [:a {:href (str "/project/add/" client-id)} "Add project"]]]))

(def colors ["D4D8D1" "A8A8A1" "AA9A66" "B74934" "577492" "67655D" "332C2F"])

(defn display-color-li [color]
  [:li {:data-value color} [:a {:href "#"} [:span {:style (str "display:inline-block;width:20px;height:15px;background-color:#" color)}] [:span.value (str "#" color) ]]])

(defn display-color-chooser [colors]
  (list
   [:button.form-control.btn.btn-default.dropdown-toggle {:type "button" :data-toggle "dropdown" :id "color-label" }
    [:span.value {} "Color"] [:span.caret]]
   [:input {:type "hidden" :id "color" :name "color"}]
   [:ul.dropdown-menu
    (map display-color-li colors)]
   [:script "$('.dropdown-menu li').click(function () {
    var color = $(this).attr('data-value');
    $('#color-label').attr('style', 'background-color:#' + color)
    $('#color').val(color);
    console.log( $('#color').val());});"]))

(defn display-add-project [client]
  [:form {:method "POST" :action (str "/project/add/" (:id client))}
   [:div.input-group-btn {:style "width:70%"}
    [:input.form-control {:type "text" :name "name" :placeholder "project name..."}]]
   [:div.input-group-btn {:style "width:10%"}
    (display-color-chooser colors)]
    [:span.input-group-btn
     [:button.btn.btn-default {:type "submit"} "Add"]]])

(defn display-add-project [client]
  [:form {:method "POST" :action (str "/project/add/" (:id client))}
   [:div.input-group-btn {:style "width:70%"}
    [:input.form-control {:type "text" :name "name" :placeholder "project name..."}]]
   [:div.input-group-btn {:style "width:10%"}
    (display-color-chooser colors)]
    [:span.input-group-btn
     [:button.btn.btn-default {:type "submit"} "Add"]]])

(defn display-edit-project [project]
  [:form {:method "POST" :action (str "/project/edit/" (:id project))}
   [:div.input-group-btn {:style "width:70%"}
    [:input.form-control {:type "text" :name "name" :value (:name project)} ]]
   [:div.input-group-btn {:style "width:10%"}
    (display-color-chooser colors)]
    [:span.input-group-btn
     [:button.btn.btn-success {:type "submit"} "Go!"]]])
