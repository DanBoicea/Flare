package com.fsociety.flarebackend.classification.llm;

/**
 * The JSON Schema the classify answer must follow (pipeline.md section 4.1). It has one shape per kind
 * ({@code anyOf}), so llama-server's grammar already stops the model from, say, attaching an answer to a Report
 * or Bouncing an Emergency. {@link ClassificationValidator} still checks everything again.
 * The model never sees this schema; the field meanings are explained in prompts/classify.system.md.
 */
public final class ClassificationSchema {

	private ClassificationSchema() {
	}

	private static final String CATEGORY = """
			{"enum":["CLEANING_OR_SPILL","TOILET","LIFT_OR_ESCALATOR","BROKEN_OR_MAINTENANCE","SIGN_OR_INFORMATION",\
			"ASSISTANCE_NEEDED","LOST_ITEM_OR_PERSON","SAFETY_OR_SECURITY","OTHER"]}""";
	private static final String COMMON = """
			"language":{"enum":["ro","en","other"]},
			"confidence":{"type":"number","minimum":0,"maximum":1},
			"mentionedPlace":{"anyOf":[{"type":"string","maxLength":80},{"type":"null"}]},
			"flightNumber":{"anyOf":[{"type":"string","maxLength":10},{"type":"null"}]}""";
	private static final String REQUIRED = """
			"required":["kind","emergency","category","severity","language","confidence","staffSummary",\
			"mentionedPlace","flightNumber","answer","answerFound","bounceReason"]""";

	public static final String JSON = """
			{"anyOf":[
			  {"type":"object","additionalProperties":false,%3$s,"properties":{
			    "kind":{"const":"REPORT"},
			    "emergency":{"type":"boolean"},
			    "category":%1$s,
			    "severity":{"enum":["CRITICAL","HIGH","MEDIUM","LOW"]},
			    %2$s,
			    "staffSummary":{"type":"object","additionalProperties":false,"required":["ro","en"],"properties":{
			      "ro":{"type":"string","minLength":1,"maxLength":140},"en":{"type":"string","minLength":1,"maxLength":140}}},
			    "answer":{"type":"null"},
			    "answerFound":{"const":false},
			    "bounceReason":{"type":"null"}}},
			  {"type":"object","additionalProperties":false,%3$s,"properties":{
			    "kind":{"const":"QUESTION"},
			    "emergency":{"const":false},
			    "category":{"type":"null"},
			    "severity":{"type":"null"},
			    %2$s,
			    "staffSummary":{"type":"null"},
			    "answer":{"type":"string","minLength":1,"maxLength":600},
			    "answerFound":{"type":"boolean"},
			    "bounceReason":{"type":"null"}}},
			  {"type":"object","additionalProperties":false,%3$s,"properties":{
			    "kind":{"const":"GARBAGE"},
			    "emergency":{"const":false},
			    "category":{"type":"null"},
			    "severity":{"type":"null"},
			    %2$s,
			    "staffSummary":{"type":"null"},
			    "answer":{"type":"null"},
			    "answerFound":{"const":false},
			    "bounceReason":{"enum":["NOT_AIRPORT_RELATED","JOKE_OR_NONSENSE","SPAM_OR_ADVERTISING","ABUSIVE","TOO_UNCLEAR"]}}}
			]}""".formatted(CATEGORY, COMMON, REQUIRED);
}
