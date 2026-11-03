package co.edu.uniquindio.bibliomtr.api.stringmatching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.bibliomtr.api.stringmatching.PatternSearchDtos.BoyerMoorePreprocessingView;
import co.edu.uniquindio.bibliomtr.api.stringmatching.PatternSearchDtos.LastOccurrenceEntry;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMoorePreprocessing;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.KmpPreprocessing;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Vista JSON del preprocesamiento de Boyer-Moore (spec 004, REQ-24): la tabla del carácter malo viaja como lista en
 * el orden de primera aparición, aunque el patrón tenga dígitos que un objeto JSON reordenaría.
 */
class PatternSearchDtosTest {

	@Test
	@DisplayName("REQ-24: la tabla del carácter malo se expone como lista en orden de primera aparición")
	void boyerMooreLastOccurrenceIsAnOrderedList() {
		Map<Character, Integer> last = new LinkedHashMap<>();
		last.put('b', 3);
		last.put('2', 2);
		last.put('1', 1);
		BoyerMoorePreprocessing preprocessing = new BoyerMoorePreprocessing("b211", last, new int[] { 3, 4, 4, 4, 5 },
				new int[] { 1, 1, 1, 1 }, 0, 4);

		Object json = PatternSearchDtos.preprocessingJson(preprocessing);

		assertThat(json).isInstanceOf(BoyerMoorePreprocessingView.class);
		BoyerMoorePreprocessingView view = (BoyerMoorePreprocessingView) json;
		assertThat(view.lastOccurrence()).containsExactly(new LastOccurrenceEntry('b', 3),
				new LastOccurrenceEntry('2', 2), new LastOccurrenceEntry('1', 1));
		assertThat(view.goodSuffixShift()).containsExactly(1, 1, 1, 1);
		assertThat(view.matchShift()).isEqualTo(4);

		JsonNode node = JsonMapper.builder().build().valueToTree(view);
		assertThat(node.get("lastOccurrence").isArray()).isTrue();
		assertThat(node.get("lastOccurrence").get(0).get("character").asString()).isEqualTo("b");
		assertThat(node.get("lastOccurrence").get(1).get("character").asString()).isEqualTo("2");
		assertThat(node.get("lastOccurrence").get(1).get("index").asInt()).isEqualTo(2);
	}

	@Test
	@DisplayName("REQ-23: el preprocesamiento de los demás algoritmos se expone sin cambios")
	void otherPreprocessingIsUnchanged() {
		KmpPreprocessing kmp = new KmpPreprocessing("ab", new int[] { 0, 0 }, List.of());

		assertThat(PatternSearchDtos.preprocessingJson(kmp)).isSameAs(kmp);
		assertThat(PatternSearchDtos.preprocessingJson(null)).isNull();
	}

}
