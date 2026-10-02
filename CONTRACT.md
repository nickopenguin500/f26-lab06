# Contract Worksheet

One section per milestone. Fill each one in as you go, in order. Write each
prediction before you run anything. That is the part a TA asks about.

Keep it short and specific. Point at methods, call sites, and error text.

---

## Milestone 1: The notes overload

### Prediction (write this before you run the build, and you can deliberate with your agent)

**Will the consumer, untouched, still compile and pass?** Yes or no.

Yes.

**Why.** What does the compiler do with the consumer's existing call sites once
the new overload exists?

The compiler binds method calls to signatures at compile time based on the method name and parameter types. The new overload `createBooking(..., String notes)` is a separate method signature. The original `createBooking` signature remains unchanged, so existing calls to it will continue to resolve to the old signature and compile perfectly.

### What happened

**The result.** What the build printed for each module.

```
[INFO] lab06-booking-parent ............................... SUCCESS [  1.010 s]
[INFO] lab06-api .......................................... SUCCESS [  2.457 s]
[INFO] lab06-consumer ..................................... SUCCESS [  0.306 s]
```

**If your prediction was wrong,** say what you missed.

N/A (Prediction was correct).

**Is an additive change always safe in Java?** One case where adding something
to an API still breaks a caller, if you can name one.

No, an additive change is not always safe. If we added a new method to `BookingApi`, any consumer that *implements* `BookingApi` would fail to compile because it wouldn't have the new method implemented. Another case: if a consumer passes `null` as an argument to an existing method and we add an overload that accepts a different type at that position, the consumer's call might become ambiguous, causing a compilation error.

---

## Milestone 2: The request object

### Prediction (write this before you run the build)

**Will the untouched consumer still compile and pass?** Yes or no, and if no,
which module goes red and whether at compile time or test time.

No, the `consumer` module will fail at compile time.

**Where.** Name the call sites you expect to be affected, if any.

`FrontDesk.java` calls `api.createBooking(...)` at lines 27 and 33. These call sites will fail to compile because the methods they rely on have been removed from `BookingApi`.

**What about the tests in `api/`, after you update them?** And whether their
result is evidence about the consumer.

The tests in `api/` will compile and pass because we will update them to use the new `BookingRequest` object. Their passing is not evidence about the consumer, because the tests are updated in sync with the API, whereas the consumer code remains on the old version.

### Step 1: after the fold

**What the build printed.** Paste it for each module, including file and
line for anything that failed.

```
[INFO] lab06-booking-parent ............................... SUCCESS [  0.068 s]
[INFO] lab06-api .......................................... SUCCESS [  0.880 s]
[INFO] lab06-consumer ..................................... FAILURE [  0.037 s]

[ERROR] /Users/nickopenguin500/Documents/GitHub/f26-lab06/consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[27,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
  required: edu.cmu.cs214.booking.BookingRequest
  found:    java.lang.String,long,long,<nulltype>
  reason: actual and formal argument lists differ in length
[ERROR] /Users/nickopenguin500/Documents/GitHub/f26-lab06/consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[33,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
  required: edu.cmu.cs214.booking.BookingRequest
  found:    java.lang.String,long,long,java.lang.String
  reason: actual and formal argument lists differ in length
```

**Which module's tests ran, and which did not.** And what that tells you about
who can detect a contract break.

The tests in `lab06-api` ran successfully. The tests in `lab06-consumer` did not run because the module failed to compile. This tells us that the API team's own tests cannot detect a contract break because they are updated alongside the API itself. Only the consumer's compiler (and tests) can detect that the contract was broken.

### Step 2: the deprecation path

**What you added.** The signatures that came back, and what they delegate to.

We brought back:
1. `Booking createBooking(String roomId, long startMinute, long endMinute, String waitlistKey)`
2. `Booking createBooking(String roomId, long startMinute, long endMinute, String waitlistKey, String notes)`
Both were marked with `@Deprecated` and now construct a `new BookingRequest(...)` using their parameters, then delegate to the new `createBooking(BookingRequest)` method.

**The warnings.** Paste one deprecation warning line from the build log (from
a `mvn -B clean test` run, since a rerun with nothing to compile prints none).

```
[WARNING] /Users/nickopenguin500/Documents/GitHub/f26-lab06/consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[27,19] createBooking(java.lang.String,long,long,java.lang.String) in edu.cmu.cs214.booking.BookingApi has been deprecated
```

**What the deprecation path resolves.** Who can now build that could not build
during step 1, and who is on which schedule.

The consumer can now build perfectly, which it couldn't during step 1. This decoupling allows the API team to ship the new `BookingRequest` surface immediately, while the consumer team can migrate their calls to the new method on their own schedule before the old one is eventually removed.

**What the warnings accomplish that a README note would not.** Be concrete
about where the warning shows up and who sees it without looking for it.

The warnings show up directly in the consumer's compiler output and their IDE (as crossed-out method names or yellow squiggles) exactly at the lines of code where they call the deprecated methods. The consumer team sees these warnings natively while doing their own work, without ever having to look at the API team's README or external documentation.

---

## Milestone 3: The misuse critique

Not coded. One misuse, one redesign, one cost. Discuss it with your TA.

### The misuse

**What is easy to get wrong.** One specific thing about the API surface.

The boolean parameter in `cancelBooking(long, boolean)` suffers from "boolean blindness". It is not obvious what the boolean flag controls.

**The call site.** File and line in `consumer/`, with the call. Show the
code that a reader cannot understand without opening the javadoc, or that a
caller could get wrong with the compiler still happy.

`FrontDesk.java` line 48: `api.cancelBooking(bookingId, true);`. A reader reading this line cannot possibly know what `true` means without opening the javadoc.

**What goes wrong when it happens.** Silent bad behavior, wrong data, a crash
somewhere far away?

A caller could easily pass `false` instead of `true` by mistake or by misunderstanding. The compiler would be happy, but the system would silently fail to promote the next guest in line, resulting in an empty room and a skipped waitlist.

### The redesign

**The proposal.** Types, enums, factories, or whatever you are proposing. Show
the new signature and the new call site.

Introduce an enum to represent the intent, such as `enum WaitlistPolicy { NOTIFY, QUIET }`. The new signature becomes `boolean cancelBooking(long bookingId, WaitlistPolicy policy)`. The call site in `FrontDesk.java` becomes `api.cancelBooking(bookingId, WaitlistPolicy.NOTIFY);`.

**Why the mistake is now hard or impossible to make.** Point at the mechanism,
such as the compiler, a validating constructor, or an exhaustive switch.

The compiler strictly enforces that an explicit `WaitlistPolicy` enum value must be passed. A caller can no longer arbitrarily pass `true` or `false`, and the explicit naming makes the developer's intent instantly obvious to any reader.

### One tradeoff

**What it costs.** Something real, such as caller ceremony, migration burden
against the deprecation path you just built, or more types for a newcomer to
learn. "No real downside" does not count.

It increases caller ceremony and expands the API surface area. The caller now has to import an additional `WaitlistPolicy` type and type out the longer enum value instead of a simple primitive boolean.

**When the price is worth paying.** A condition under which it is.

This price is worth paying when code readability is critical (e.g., during code reviews) and when the consequences of silently executing the wrong business logic (like dropping waiting customers) are severe.
