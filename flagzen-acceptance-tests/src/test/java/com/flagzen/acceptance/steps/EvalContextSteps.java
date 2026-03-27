package com.flagzen.acceptance.steps;

import com.flagzen.EvaluationContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.datatable.DataTable;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for EvaluationContext builder scenarios (US-EC-01).
 */
public class EvalContextSteps {

    private EvaluationContext context;

    @Given("a developer needs to target flags for user {string}")
    public void aDeveloperNeedsToTargetFlagsForUser(String userId) {
        // Context: the developer intends to build a context for this user.
        // Actual building happens in the "When" step.
    }

    @When("the developer builds an evaluation context with targeting key {string} and attributes:")
    public void theDeveloperBuildsContextWithKeyAndAttributes(String targetingKey, DataTable dataTable) {
        EvaluationContext.Builder builder = EvaluationContext.builder()
                .targetingKey(targetingKey);
        for (Map<String, String> row : dataTable.asMaps()) {
            builder.attribute(row.get("attribute"), row.get("value"));
        }
        context = builder.build();
    }

    @When("the developer builds an evaluation context with only attribute {string} = {string}")
    public void theDeveloperBuildsContextWithOnlyAttribute(String key, String value) {
        context = EvaluationContext.builder()
                .attribute(key, value)
                .build();
    }

    @When("the developer builds an evaluation context with targeting key {string} and no attributes")
    public void theDeveloperBuildsContextWithKeyOnly(String targetingKey) {
        context = EvaluationContext.builder()
                .targetingKey(targetingKey)
                .build();
    }

    @Then("the context targeting key is {string}")
    public void theContextTargetingKeyIs(String expected) {
        assertThat(context.targetingKey()).isEqualTo(expected);
    }

    @Then("the context targeting key is absent")
    public void theContextTargetingKeyIsAbsent() {
        assertThat(context.targetingKey()).isNull();
    }

    @Then("the context attribute {string} is {string}")
    public void theContextAttributeIs(String key, String expected) {
        assertThat(context.attributes()).containsEntry(key, expected);
    }

    @Then("the context has no attributes")
    public void theContextHasNoAttributes() {
        assertThat(context.attributes()).isEmpty();
    }

    @Then("the context has an empty attributes collection, not null")
    public void theContextHasEmptyAttributesNotNull() {
        assertThat(context.attributes()).isNotNull().isEmpty();
    }
}
