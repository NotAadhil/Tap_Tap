package com.tapflow.automation

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ActionValidatorTest {

    @Test
    fun validateTagName_acceptsValidNames() {
        assertNull(ActionValidator.validateTagName("Study Mode"))
        assertNull(ActionValidator.validateTagName("Work Desk NFC"))
    }

    @Test
    fun validateTagName_rejectsBlankOrTooLong() {
        assertNotNull(ActionValidator.validateTagName(""))
        assertNotNull(ActionValidator.validateTagName("   "))
        assertNotNull(ActionValidator.validateTagName(null))
        assertNotNull(ActionValidator.validateTagName("A".repeat(51)))
    }

    @Test
    fun validateUrl_acceptsValidHttpAndHttps() {
        assertNull(ActionValidator.validateUrl("https://developer.android.com"))
        assertNull(ActionValidator.validateUrl("http://example.com/test"))
        assertNull(ActionValidator.validateUrl("https://github.com/android"))
    }

    @Test
    fun validateUrl_rejectsInvalidOrMissingScheme() {
        assertNotNull(ActionValidator.validateUrl(""))
        assertNotNull(ActionValidator.validateUrl("example.com"))
        assertNotNull(ActionValidator.validateUrl("ftp://files.example.com"))
        assertNotNull(ActionValidator.validateUrl("https://"))
        assertNotNull(ActionValidator.validateUrl(null))
    }
}
