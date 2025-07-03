# Sentry Performance Instrumentation Implementation Documentation

  

## Overview

  

This document outlines the implementation of Sentry's custom performance APIs and methods in the SnoutAndAbout Android application to track User Registration flows and calculate Apdex scores for individual screens. The implementation provides comprehensive performance monitoring across the entire registration journey.

  

## Architecture Overview

  

### Multi-Level Transaction Hierarchy

  

The application implements a sophisticated multi-level transaction hierarchy:

  

1.  **App Session Transaction** - Top-level transaction spanning the entire app session

2.  **Registration Journey Transaction** - User journey transaction for the complete registration flow

3.  **Screen-Level Spans** - Individual spans for each registration step

4.  **Process Spans** - Granular spans for specific operations within each step

  

### Transaction Flow Structure

  

```

App Session Transaction (SentryInitProvider)

└── Registration Journey Transaction (MainActivity)

├── Registration Steps Flow Span

│ ├── Step 1: Waiting for Input Span (MainActivity)

│ ├── Step 1: Process Input Span (MainActivity)

│ ├── Step 2: Dog Profile Screen Span (DogProfileActivity)

│ │ └── Step 2: Process Dog Profile Span

│ └── Step 3: Hydrant Preferences Screen Span (HydrantPreferencesActivity)

│ └── Step 3: Preferences Submit Span

```

  

## Implementation Details

  

### 1. Sentry Initialization

  

#### Configuration Location

-  **File**: `app/src/main/AndroidManifest.xml`

-  **Provider**: `SentryInitProvider.java`

-  **Application**: `MyApplication.java`

  

#### Key Configuration Parameters

  

```xml

<!-- Performance API Configuration -->

<meta-data

android:name="io.sentry.traces.sample-rate"

android:value="1.0"  />

<meta-data

android:name="io.sentry.traces.profiling.session-sample-rate"

android:value="1.0"  />

<meta-data

android:name="io.sentry.traces.profiling.lifecycle"

android:value="trace"  />

<meta-data

android:name="io.sentry.traces.profiling.start-on-app-start"

android:value="true"  />

```

  

#### SentryInitProvider Implementation

  

The `SentryInitProvider` ensures Sentry is initialized before the Application.onCreate() method:

  

```java

// Initialize Sentry with performance monitoring

SentryAndroid.init(context, options ->  {

options.setDsn("https://514d5e38a4003c2cf958c53c77679925@o4508236363464704.ingest.us.sentry.io/4508847444918272");

options.setEnableAutoActivityLifecycleTracing(true);

options.setEnableUserInteractionTracing(true);

options.setTracesSampleRate(1.0);

options.setRelease("5.0");

options.setDebug(true);

});

  

// Create app session transaction

appSessionTransaction = Sentry.startTransaction("App Session",  "app.session");

```

  

### 2. Registration Journey Transaction Management

  

#### Transaction Creation (MainActivity)

  

```java

// Create the main registration journey transaction

registrationTransaction = Sentry.startTransaction("Registration Journey",  "user_journey.registration");

Sentry.getCurrentScopes().getScope().setTransaction(registrationTransaction);

  

// Create child spans for the registration flow

registrationStepsSpan = registrationTransaction.startChild("registration_steps_flow",  "registration.steps");

waitingForUserInputSpan = registrationStepsSpan.startChild("step1.waiting_for_input",  "ui.wait");

```

  

#### Transaction Continuity Across Activities

  

The implementation maintains transaction continuity across multiple activities using Intent extras:

  

```java

// Pass transaction context to next activity

intent.putExtra("sentry_trace_id", registrationStepsSpan.getSpanContext().getTraceId().toString());

intent.putExtra("sentry_span_id", registrationStepsSpan.getSpanContext().getSpanId().toString());

intent.putExtra("sentry_parent_span_id", registrationStepsSpan.getSpanContext().getParentSpanId().toString());

intent.putExtra("sentry_transaction_name",  "Registration Journey");

intent.putExtra("sentry_transaction_operation",  "user_journey.registration");

```

  

### 3. Screen-Level Performance Tracking

  

#### Step 1: MainActivity (Email/Password Input)

  

**Spans Created:**

-  `step1.waiting_for_input` - Tracks time user spends on input fields

-  `step1.process_input` - Tracks input validation and processing

  

**Performance Metrics:**

- User input time

- Validation processing time

- Total step completion time

  

#### Step 2: DogProfileActivity

  

**Spans Created:**

-  `step2.dog_profile_screen` - Tracks entire screen duration

-  `step2.process_dog_profile` - Tracks form processing and validation

  

**Transaction Continuity:**

```java

// Continue existing transaction or create new one with custom context

if  (registrationTransaction !=  null  && registrationTransaction.getName().equals(transactionName))  {

dogProfileActivitySpan = registrationTransaction.startChild("step2.dog_profile_screen",  "registration.step2");

}  else  {

// Create new transaction with propagation context

PropagationContext propagationContext =  new  PropagationContext(

new  SentryId(traceId),

new  SpanId(parentSpanId),

new  SpanId(),

null,

null

);

// ... transaction creation with custom context

}

```

  

#### Step 3: HydrantPreferencesActivity

  

**Spans Created:**

-  `step3.hydrant_preferences_screen` - Tracks screen duration

-  `step3.preferences_submit` - Tracks final submission processing

  

**Transaction Completion:**

```java

// Finish the main registration journey transaction

registrationTransaction.finish(SpanStatus.OK);

  

// Also finish the App Session transaction

MyApplication.finishAppSession();

```

  

### 4. Error Handling and Cancellation

  

#### Graceful Cancellation Handling

  

The implementation properly handles user cancellations and errors:

  

```java

@Override

public  void  onBackPressed()  {

setResult(RESULT_CANCELED);

super.onBackPressed();

// Mark span as cancelled

if  (dogProfileActivitySpan !=  null  &&  !dogProfileActivitySpan.isFinished())  {

dogProfileActivitySpan.finish(SpanStatus.CANCELLED);

}

}

```

  

#### Activity Result Handling

  

```java

// Handle activity results and update transaction status accordingly

if  (result.getResultCode()  == RESULT_OK)  {

// Continue with registration flow

}  else  if  (result.getResultCode()  == RESULT_CANCELED)  {

// Cancel entire registration transaction

if  (registrationTransaction !=  null  &&  !registrationTransaction.isFinished())  {

registrationTransaction.finish(SpanStatus.CANCELLED);

}

}

```