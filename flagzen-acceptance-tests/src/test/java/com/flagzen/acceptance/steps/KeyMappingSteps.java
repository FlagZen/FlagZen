package com.flagzen.acceptance.steps;

import com.flagzen.keymapping.FlagKeyFormat;
import com.flagzen.keymapping.FlagKeyFormats;
import com.flagzen.keymapping.FlagKeyParser;
import com.flagzen.keymapping.FlagKeyParsers;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for key mapping parser and formatter acceptance tests.
 */
public class KeyMappingSteps {

    private FlagKeyParser parser;
    private FlagKeyFormat formatter;
    private Optional<List<String>> parseResult;
    private String formatResult;

    @Before("@US-ENV-05 or @US-ENV-06")
    public void resetState() {
        parser = null;
        formatter = null;
        parseResult = null;
        formatResult = null;
    }

    // --- Parser Given steps ---

    @Given("a screaming snake case parser with prefix {string}")
    public void aScreamingSnakeCaseParserWithPrefix(String prefix) {
        parser = FlagKeyParsers.screamingSnakeCase(prefix);
    }

    @Given("a screaming snake case parser without prefix")
    public void aScreamingSnakeCaseParserWithoutPrefix() {
        parser = FlagKeyParsers.screamingSnakeCase();
    }

    @Given("a camel case parser with prefix {string}")
    public void aCamelCaseParserWithPrefix(String prefix) {
        parser = FlagKeyParsers.camelCase(prefix);
    }

    @Given("a camel case parser without prefix")
    public void aCamelCaseParserWithoutPrefix() {
        parser = FlagKeyParsers.camelCase();
    }

    // --- Parser When steps ---

    @When("it parses the name {string}")
    public void itParsesTheName(String name) {
        parseResult = parser.parse(name);
    }

    // --- Parser Then steps ---

    @Then("the segments are {string} and {string}")
    public void theSegmentsAreAnd(String first, String second) {
        assertThat(parseResult)
                .isPresent()
                .hasValue(List.of(first, second));
    }

    @Then("no segments are returned")
    public void noSegmentsAreReturned() {
        assertThat(parseResult).isEmpty();
    }

    @Then("the only segment is {string}")
    public void theOnlySegmentIs(String segment) {
        assertThat(parseResult)
                .isPresent()
                .hasValue(List.of(segment));
    }

    // --- Formatter Given steps ---

    @Given("a kebab case formatter")
    public void aKebabCaseFormatter() {
        formatter = FlagKeyFormats.kebabCase();
    }

    @Given("a snake case formatter")
    public void aSnakeCaseFormatter() {
        formatter = FlagKeyFormats.snakeCase();
    }

    @Given("a camel case formatter")
    public void aCamelCaseFormatter() {
        formatter = FlagKeyFormats.camelCase();
    }

    @Given("a pascal case formatter")
    public void aPascalCaseFormatter() {
        formatter = FlagKeyFormats.pascalCase();
    }

    @Given("a dot case formatter")
    public void aDotCaseFormatter() {
        formatter = FlagKeyFormats.dotCase();
    }

    @Given("a colon case formatter")
    public void aColonCaseFormatter() {
        formatter = FlagKeyFormats.colonCase();
    }

    @Given("a custom formatter that joins segments with {string}")
    public void aCustomFormatterThatJoinsSegmentsWith(String delimiter) {
        formatter = segments -> String.join(delimiter, segments);
    }

    // --- Formatter When steps ---

    @When("it formats the segments {string} and {string}")
    public void itFormatsTheSegmentsAnd(String first, String second) {
        formatResult = formatter.format(List.of(first, second));
    }

    @When("it formats the single segment {string}")
    public void itFormatsTheSingleSegment(String segment) {
        formatResult = formatter.format(List.of(segment));
    }

    // --- Formatter Then steps ---

    @Then("the flag key is {string}")
    public void theFlagKeyIs(String expectedKey) {
        assertThat(formatResult).isEqualTo(expectedKey);
    }
}
