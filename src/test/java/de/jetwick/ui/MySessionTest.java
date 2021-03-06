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
package de.jetwick.ui;

import de.jetwick.tw.Twitter4JUser;
import de.jetwick.es.ElasticUserSearch;
import de.jetwick.data.JUser;
import de.jetwick.tw.TwitterSearch;
import javax.servlet.http.Cookie;
import org.apache.wicket.Request;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

/**
 *
 * @author Peter Karich, jetwick_@_pannous_._info
 */
public class MySessionTest extends WicketPagesTestClass {

    public MySessionTest() {
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected MySession changeSession(MySession sess, Request req) {
        return new MySession(req);
    }

    ElasticUserSearch newMockUserSearch(JUser user) {
        ElasticUserSearch s = mock(ElasticUserSearch.class);
        when(s.findByTwitterToken("normalToken")).thenReturn(user);
        return s;
    }

    @Test
    public void testInit() {
        MySession session = (MySession) tester.getWicketSession();
        session.invalidate();
        assertNull(session.getUser());

        session.onNewSession(tester.getWicketRequest(), newMockUserSearch(null));
        assertNull(session.getUser());
        session.logout(newMockUserSearch(null), tester.getWicketResponse(), true);
        assertNull(session.getUser());
    }

    @Test
    public void testInitFromCookie() {
        MySession session = (MySession) tester.getWicketSession();
        WebRequest req = mock(WebRequest.class);
        when(req.getCookie(TwitterSearch.COOKIE)).thenReturn(new Cookie(TwitterSearch.COOKIE, "normalToken"));
        session.onNewSession(req, newMockUserSearch(new JUser("testuser")));
        assertEquals("testuser", session.getUser().getScreenName());
    }

    @Test
    public void testDoNotInitFromWrongCookie() {
        MySession session = (MySession) tester.getWicketSession();
        WebRequest req = mock(WebRequest.class);
        when(req.getCookie(TwitterSearch.COOKIE)).thenReturn(new Cookie("tokenWrong", null));
        session.onNewSession(req, newMockUserSearch(new JUser("testuser")));
        assertNull(session.getUser());
    }

    @Test
    public void testSetCookie() throws TwitterException {
        TwitterSearch ts = mock(TwitterSearch.class);
        when(ts.initTwitter4JInstance("normalToken", "tSec", true)).thenReturn(ts);
        //when(ts.getCredits()).thenReturn(new Credits("normalToken", "tSec", "x", "y"));
        when(ts.getTwitterUser()).thenReturn(new Twitter4JUser("testuser"));

        WebResponse resp = mock(WebResponse.class);
        JUser user = new JUser("testuser");
        ElasticUserSearch uSearch = newMockUserSearch(user);
        MySession session = (MySession) tester.getWicketSession();
        session.setTwitterSearch(ts);
        session.setFormData("tmp@tmp.de", "test");
        // token starts with user id!
        Cookie cookie = session.afterLogin(new AccessToken("123-normalToken", "tSec"), uSearch, resp);
        verify(uSearch).save(user, false);
        assertEquals(TwitterSearch.COOKIE, cookie.getName());
        assertEquals("123-normalToken", cookie.getValue());

        uSearch = newMockUserSearch(user);
        session.logout(uSearch, resp, true);
        verify(uSearch).save(user, false);
        //verify(resp).clearCookie(new Cookie(TwitterSearch.COOKIE, ""));
    }

    @Test
    public void testFailingWithoutEmail() throws TwitterException {
        TwitterSearch ts = mock(TwitterSearch.class);
        when(ts.initTwitter4JInstance("normalToken", "tSec", true)).thenReturn(ts);
        //when(ts.getCredits()).thenReturn(new Credits("normalToken", "tSec", "x", "y"));
        when(ts.getTwitterUser()).thenReturn(new Twitter4JUser("testuser"));

        WebResponse resp = mock(WebResponse.class);
        JUser user = new JUser("testuser");
        ElasticUserSearch uSearch = newMockUserSearch(user);
        MySession session = (MySession) tester.getWicketSession();
        session.setTwitterSearch(ts);
        try {
            session.afterLogin(new AccessToken("123-normalToken", "tSec"), uSearch, resp);
            assertTrue(false);
        } catch (IllegalArgumentException ex) {
            assertTrue(true);
        }
    }
}
