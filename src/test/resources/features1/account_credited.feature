@regression
Feature: Account is credited with amount

@SC1
Scenario: Credit amount1
Given account balance is 0.0
When the account is credited with 10.0
Then account should have a balance of 10.0

@SC2
Scenario: Credit amount2
Given account balance is 0.0
When the account is credited with 11.0
Then account should have a balance of 11.0