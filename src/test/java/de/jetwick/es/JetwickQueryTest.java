/*
 *  Copyright 2010 Peter Karich jetwick_@_pannous_._info
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.jetwick.es;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.ExistsFilterBuilder;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.index.query.TermFilterBuilder;
import org.elasticsearch.index.query.TermsFilterBuilder;
import de.jetwick.data.JTweet;
import de.jetwick.data.JUser;
import java.io.IOException;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.elasticsearch.common.xcontent.XContentFactory.*;

/**
 *
 * @author Peter Karich, jetwick_@_pannous_._info
 */
public class JetwickQueryTest {

    public JetwickQueryTest() {
    }

    @Test
    public void testForbiddenChars() {
        assertFalse(JetwickQuery.containsForbiddenChars("test schnest"));
        assertTrue(JetwickQuery.containsForbiddenChars("test:xy"));
        assertTrue(JetwickQuery.containsForbiddenChars("test:xy*"));
    }

    @Test
    public void testParseDate() {
        TweetQuery newQ = TweetQuery.parseQuery("q=test&fq=dt:[2011-03-21T09:00:00.0Z TO *]&fq=x:y");
        assertEquals("dt", newQ.getFilterQueries().get(0).getKey());
        assertEquals("[2011-03-21T09:00:00.0Z TO *]", newQ.getFilterQueries().get(0).getValue());
        assertEquals("x", newQ.getFilterQueries().get(1).getKey());
        assertEquals("y", newQ.getFilterQueries().get(1).getValue());
    }

    @Test
    public void testRemoveFilters() {
        JetwickQuery q = new TweetQuery().addFilterQuery("test", "pest");
        q.removeFilterQueries("test");
        assertEquals(0, q.getFilterQueries().size());
        q.addFilterQuery("test", "pest").addFilterQuery("-test", "pesting").addFilterQuery("anotherfield", "value");
        q.removeFilterQueries("test");
        assertEquals(1, q.getFilterQueries().size());
        assertEquals("anotherfield", q.getFilterQueries().get(0).getKey());
    }

    @Test
    public void testSimilarQuery() {
        SimilarTweetQuery q = new SimilarTweetQuery(
                new JTweet(1L, "Test test jAva http://blabli", new JUser("tmp")), false);

        assertTrue(q.calcTerms().contains("test"));
        assertTrue(q.calcTerms().contains("java"));
        assertFalse("query mustn't contain links or parts of links", q.calcTerms().contains("http"));
        q = new SimilarTweetQuery(new JTweet(1L, "RT @user: test", new JUser("tmp")), false);
        assertFalse("query mustn't contain user", q.calcTerms().contains("user"));
    }

    @Test
    public void testParse() {
        JetwickQuery q = new TweetQuery("test").addFilterQuery("test", "xy").
                setSort("blie", "desc").addFacetField("coolField").addFacetField("test", 20);
        TweetQuery newQ = TweetQuery.parseQuery(q.toString());
        assertEquals(q.toString(), newQ.toString());
        assertEquals(q, newQ);
    }

    @Test
    public void testFilterQuery2Builder() throws IOException {
        // how to test???

        FilterBuilder builder = new TweetQuery().filterQuery2Builder("field", "[1 TO 2]");
        assertEquals(1, 1);
        builder = new TweetQuery().filterQuery2Builder("field", "[1 TO Infinity]");
        assertTrue(builder instanceof RangeFilterBuilder);
        assertEquals(c("{'range':{'field':{'from':1,'to':null,'include_lower':true,'include_upper':true}}}"), toString(builder));

        builder = new TweetQuery().filterQuery2Builder("field", "[-Infinity TO Infinity]");
        assertTrue(builder instanceof ExistsFilterBuilder);

        builder = new TweetQuery().filterQuery2Builder("field", "[-Infinity TO 2]");
        assertTrue(builder instanceof RangeFilterBuilder);
        assertEquals(c("{'range':{'field':{'from':null,'to':2,'include_lower':true,'include_upper':true}}}"), toString(builder));

        builder = new TweetQuery().filterQuery2Builder("field", "test");
        assertTrue(builder instanceof TermFilterBuilder);
        assertEquals(c("{'term':{'field':'test'}}"), toString(builder));

        builder = new TweetQuery().filterQuery2Builder("field", "\"test\"");
        assertTrue(builder instanceof TermFilterBuilder);
        assertEquals(c("{'term':{'field':'test'}}"), toString(builder));

        builder = new TweetQuery().filterQuery2Builder("field", "1 OR 2");
        assertTrue(builder instanceof TermsFilterBuilder);
        assertEquals(c("{'terms':{'field':[1,2]}}"), toString(builder));
    }

    public static String c(String str) {
        return str.replaceAll("'", "\"");
    }

    public static String toString(ToXContent content) throws IOException {
        XContentBuilder json = jsonBuilder();
        content.toXContent(json, null); //ToXContent.EMPTY_PARAMS        
        return json.string();
    }
}
