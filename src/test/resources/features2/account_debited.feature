@regression
Feature: Account is debited with amount

@SC3
Scenario: Debit amount1
Given account balance is 10.0
When the account is debited with 2.0
Then account should have a balance of 8.0

@SC4
Scenario: Debit amount2
Given account balance is 8.0
When the account is debited with 1.0
Then account should have a balance of 7.0