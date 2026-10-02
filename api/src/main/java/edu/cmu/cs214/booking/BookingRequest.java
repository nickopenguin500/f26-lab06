package edu.cmu.cs214.booking;

public final class BookingRequest {
    private final String roomId;
    private final long startMinute;
    private final long endMinute;
    private final String waitlistKey;
    private final String notes;

    public BookingRequest(String roomId, long startMinute, long endMinute,
                          String waitlistKey, String notes) {
        this.roomId = roomId;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.waitlistKey = waitlistKey;
        this.notes = notes;
    }

    public String getRoomId() {
        return roomId;
    }

    public long getStartMinute() {
        return startMinute;
    }

    public long getEndMinute() {
        return endMinute;
    }

    public String getWaitlistKey() {
        return waitlistKey;
    }

    public String getNotes() {
        return notes;
    }
}

