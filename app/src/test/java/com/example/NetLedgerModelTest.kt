package com.example

import com.example.data.model.BillingFormatters
import com.example.data.model.PaymentMethod
import com.example.data.model.Subscriber
import com.example.data.model.SubscriberStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetLedgerModelTest {

    @Test
    fun `cnic formatting inserts dashes at 5 and 12 digits`() {
        assertEquals("35202-1234567-1", BillingFormatters.formatCnicInput("3520212345671"))
        assertEquals("35202-123", BillingFormatters.formatCnicInput("35202123"))
        assertEquals("35202-1234567-1", BillingFormatters.formatCnicInput("35202-1234567-1999"))
    }

    @Test
    fun `subscriber create map excludes id and includes all rule-required keys`() {
        val sub = Subscriber(
            id = "doc_123",
            subscriberCode = "NET-1001",
            fullName = "Zain Ali",
            phoneNumber = "0300-1112233",
            cnic = "35202-1112233-1",
            location = "House 9, Street 4",
            areaZone = "Gulberg",
            packageName = "30 Mbps Fiber",
            status = SubscriberStatus.NEW.name,
            monthlyBill = 2500L,
            pendingBill = 2500L,
            totalPaid = 0L,
            lastBilledMonth = "2026-11",
            notes = "ONU near gate"
        )
        val map = sub.toCreateMap("uid_abc")
        assertFalse(map.containsKey("id"))
        assertEquals("uid_abc", map["userId"])
        assertEquals("NET-1001", map["subscriberCode"])
        assertEquals(16, map.size)
        assertTrue(map.containsKey("createdAt"))
        assertTrue(map.containsKey("updatedAt"))
    }

    @Test
    fun `enum parsing falls back safely on unknown raw values`() {
        assertEquals(SubscriberStatus.ACTIVE, SubscriberStatus.fromRaw("UNKNOWN_STATUS"))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromRaw("CRYPTO"))
    }
}
