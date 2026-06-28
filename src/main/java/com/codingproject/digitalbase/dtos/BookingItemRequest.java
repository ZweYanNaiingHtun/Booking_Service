//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.dtos;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Generated;

public class BookingItemRequest {
    private @NotNull(
            message = "Service ID is required"
    ) Long serviceId;
    private @NotNull(
            message = "Booking date is required"
    ) @Future(
            message = "Booking date must be in the future"
    ) Instant bookingDate;
    private Long requestedStaffId;

    @Generated
    public BookingItemRequest() {
    }

    @Generated
    public Long getServiceId() {
        return this.serviceId;
    }

    @Generated
    public Instant getBookingDate() {
        return this.bookingDate;
    }

    @Generated
    public Long getRequestedStaffId() {
        return this.requestedStaffId;
    }

    @Generated
    public void setServiceId(final Long serviceId) {
        this.serviceId = serviceId;
    }

    @Generated
    public void setBookingDate(final Instant bookingDate) {
        this.bookingDate = bookingDate;
    }

    @Generated
    public void setRequestedStaffId(final Long requestedStaffId) {
        this.requestedStaffId = requestedStaffId;
    }

    @Generated
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof BookingItemRequest)) {
            return false;
        } else {
            BookingItemRequest other = (BookingItemRequest)o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                Object this$serviceId = this.getServiceId();
                Object other$serviceId = other.getServiceId();
                if (this$serviceId == null) {
                    if (other$serviceId != null) {
                        return false;
                    }
                } else if (!this$serviceId.equals(other$serviceId)) {
                    return false;
                }

                Object this$requestedStaffId = this.getRequestedStaffId();
                Object other$requestedStaffId = other.getRequestedStaffId();
                if (this$requestedStaffId == null) {
                    if (other$requestedStaffId != null) {
                        return false;
                    }
                } else if (!this$requestedStaffId.equals(other$requestedStaffId)) {
                    return false;
                }

                Object this$bookingDate = this.getBookingDate();
                Object other$bookingDate = other.getBookingDate();
                if (this$bookingDate == null) {
                    if (other$bookingDate != null) {
                        return false;
                    }
                } else if (!this$bookingDate.equals(other$bookingDate)) {
                    return false;
                }

                return true;
            }
        }
    }

    @Generated
    protected boolean canEqual(final Object other) {
        return other instanceof BookingItemRequest;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $serviceId = this.getServiceId();
        result = result * 59 + ($serviceId == null ? 43 : $serviceId.hashCode());
        Object $requestedStaffId = this.getRequestedStaffId();
        result = result * 59 + ($requestedStaffId == null ? 43 : $requestedStaffId.hashCode());
        Object $bookingDate = this.getBookingDate();
        result = result * 59 + ($bookingDate == null ? 43 : $bookingDate.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        Long var10000 = this.getServiceId();
        return "BookingItemRequest(serviceId=" + var10000 + ", bookingDate=" + String.valueOf(this.getBookingDate()) + ", requestedStaffId=" + this.getRequestedStaffId() + ")";
    }
}
