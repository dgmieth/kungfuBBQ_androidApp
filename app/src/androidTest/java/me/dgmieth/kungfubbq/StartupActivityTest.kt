package me.dgmieth.kungfubbq


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class StartupActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(StartupActivity::class.java)

    @Test
    fun startupActivityTest() {
        val appCompatButton = onView(
            allOf(
                withId(R.id.homeLoginBtn), withText("Log In"),
                childAtPosition(
                    allOf(
                        withId(R.id.homeFragment),
                        childAtPosition(
                            withId(R.id.nav_host_fragment),
                            0
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatButton.perform(click())

        val appCompatEditText = onView(
            allOf(
                withId(R.id.loginUserEmail),
                childAtPosition(
                    allOf(
                        withId(R.id.loginFragment),
                        childAtPosition(
                            withId(R.id.nav_host_fragment),
                            0
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatEditText.perform(replaceText("dgmieth@gmail.com"), closeSoftKeyboard())

        val appCompatEditText2 = onView(
            allOf(
                withId(R.id.loginPassword),
                childAtPosition(
                    allOf(
                        withId(R.id.loginFragment),
                        childAtPosition(
                            withId(R.id.nav_host_fragment),
                            0
                        )
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        appCompatEditText2.perform(replaceText("12312345"), closeSoftKeyboard())

        val appCompatEditText3 = onView(
            allOf(
                withId(R.id.loginPassword), withText("12312345"),
                childAtPosition(
                    allOf(
                        withId(R.id.loginFragment),
                        childAtPosition(
                            withId(R.id.nav_host_fragment),
                            0
                        )
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        appCompatEditText3.perform(pressImeActionButton())

        val appCompatButton2 = onView(
            allOf(
                withId(R.id.loginLoginBtn), withText("Log In"),
                childAtPosition(
                    allOf(
                        withId(R.id.btnsView),
                        childAtPosition(
                            withId(R.id.loginFragment),
                            8
                        )
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        appCompatButton2.perform(click())

        val appCompatButton3 = onView(
            allOf(
                withId(R.id.homeCalendarBtn), withText("Calendar"),
                childAtPosition(
                    allOf(
                        withId(R.id.homeFragment),
                        childAtPosition(
                            withId(R.id.nav_host_fragment),
                            0
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatButton3.perform(click())

        val frameLayout = onView(
            allOf(
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.LinearLayout")),
                        3
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        frameLayout.perform(click())

        val appCompatButton4 = onView(
            allOf(
                withId(R.id.calendarUpdateOrder), withText("Update"),
                childAtPosition(
                    allOf(
                        withId(R.id.calendarFragment),
                        childAtPosition(
                            withId(R.id.nav_host_fragment),
                            0
                        )
                    ),
                    9
                ),
                isDisplayed()
            )
        )
        appCompatButton4.perform(click())

        val actionMenuItemView = onView(
            allOf(
                withId(R.id.editMenuBtn), withContentDescription("Edit"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.toolbar),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        actionMenuItemView.perform(click())

        val appCompatButton5 = onView(
            allOf(
                withId(R.id.updateOrderUpdateOrderBtn), withText("Update"),
                childAtPosition(
                    allOf(
                        withId(R.id.updateOrderUpdateOrderBtns),
                        childAtPosition(
                            withId(R.id.preOrderFragment),
                            15
                        )
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        appCompatButton5.perform(click())

        val appCompatButton6 = onView(
            allOf(
                withId(android.R.id.button1), withText("Ok"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        )
        appCompatButton6.perform(scrollTo(), click())

        val actionMenuItemView2 = onView(
            allOf(
                withId(R.id.userInfoMenuBtn), withContentDescription("User info"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.toolbar),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        actionMenuItemView2.perform(click())

        val actionMenuItemView3 = onView(
            allOf(
                withId(R.id.logOutMenuBtn), withContentDescription("Log out"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.toolbar),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        actionMenuItemView3.perform(click())
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
