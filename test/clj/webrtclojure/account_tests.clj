(ns webrtclojure.account-tests
  (:require [clojure.test :refer :all]
            [webrtclojure.accounts :as accounts]))

(defn clean-users [f]
  (accounts/delete-all)
  (f))

(use-fixtures :each clean-users)

(deftest authorize-account
  (let [user (accounts/create {:username "Alf" :email "alf@example.com" :password "s3cr3t"})
        user-id (:id user)]
    (testing "Accepts the correct password"
      (is (accounts/correct-password? user-id "s3cr3t")))

    (testing "Rejects incorrect passwords"
      (is (not (accounts/correct-password? user-id "not_my_password"))))

    (testing "Clean up test account"
      (is accounts/delete-user user-id))))
