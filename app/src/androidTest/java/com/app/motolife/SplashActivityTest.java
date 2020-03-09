package com.app.motolife;

import com.example.motolife.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ActivityScenario;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4ClassRunner.class)
public class SplashActivityTest {

    @Test
    public void test_isActivityInView() {
        ActivityScenario.launch(SplashActivity.class);
        onView(withId(R.id.splash_activity_layout))
                .check(matches(isDisplayed()));
    }

    @Test
    public void test_visibilityAnimatedLogo() {
        ActivityScenario.launch(SplashActivity.class);
        onView(withId(R.id.motoLifeLogo))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    @Test
    public void test_isProgressTextDisplayed() {
        ActivityScenario.launch(SplashActivity.class);
        onView(withId(R.id.checkProgressText))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }
}