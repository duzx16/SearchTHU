import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class SuggestResults {
    public String[] queries;
}


public class THUSuggester {
    private AnalyzingInfixSuggester suggester;

    public THUSuggester(Directory directory, Analyzer analyzer, IndexReader reader) throws IOException {
        LuceneDictionary content_dictionary = new LuceneDictionary(reader, "content");
        suggester = new AnalyzingInfixSuggester(directory, analyzer);
        suggester.build(content_dictionary.getEntryIterator());
    }

    public SuggestResults suggest(String query) {
        SuggestResults suggestResults = new SuggestResults();
        List<Lookup.LookupResult> results;
        try {
            results = suggester.lookup(query, 10, false, false);
        } catch (IOException e) {
            System.out.println("IO Exception when suggest");
            return null;
        }
        List<String> suggestions = new ArrayList<>();
        for (Lookup.LookupResult result : results) {
            suggestions.add(result.key.toString());
        }
        suggestResults.queries = suggestions.toArray(new String[0]);
        return suggestResults;
    }
}
