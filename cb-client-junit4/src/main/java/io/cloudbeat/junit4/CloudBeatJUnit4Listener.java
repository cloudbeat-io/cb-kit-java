/*
 *  Copyright 2019 Qameta Software OÜ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.cloudbeat.junit4;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;


/**
 * CloudBeat reporter plugin for JUnit 4.
 */
@RunListener.ThreadSafe
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "ClassFanOutComplexity",
        "ClassDataAbstractionCoupling",
        "unused"
})
public class CloudBeatJUnit4Listener extends RunListener {
    public CloudBeatJUnit4Listener() {
        System.out.println("CloudBeatJUnit4Listener");
    }
    @Override
    public void testRunStarted(final Description description) {
        //do nothing
    }

    @Override
    public void testRunFinished(final Result result) {
        //do nothing
    }

    @Override
    public void testStarted(final Description description) {

    }

    @Override
    public void testFinished(final Description description) {

    }

    @Override
    public void testFailure(final Failure failure) {

    }

    @Override
    public void testAssumptionFailure(final Failure failure) {

    }

    @Override
    public void testIgnored(final Description description) {

    }
}