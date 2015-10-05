Global Entry Integration Service
================================
Importing published "application submissions" formatted as JSON from RabbitMQ into Registered Traveller Caseworker and interfacing with Global Entry modules which deal with USA web services.

Application built with the following (main) technologies:

- Scala

- SBT

- Akka

- RabbitMQ

- Spray

- MongoDB

Introduction
------------
TODO

Application
-----------
The application is configured as per any Akka application, where the default configuration file is "application.conf".
This default file can be overridden with other "conf" files and then given to the application upon boot with the following example Java option:
> -Dconfig.file=test-classes/application.test.conf

Individual configuration properties can be overridden again by Java options e.g. to override which Mongodb to connect:
> -Dmongo.db=some-other-mongo

where this overrides the default in application.conf.

Booting the application (uk.gov.homeoffice.vet.registeredtraveller.Boot) exposes a RESTful API e.g. the following URL could be given in a browser, a browser's REST client plugin or curl:
> http://localhost:9100/registered-traveller

this would get back JSON that simply shows the application is alive.

Build and Deploy
----------------
The project is built with SBT. On a Mac (sorry everyone else) do:
> brew install sbt

It is also a good idea to install Typesafe Activator (which sits on top of SBT) for when you need to create new projects - it also has some SBT extras, so running an application with Activator instead of SBT can be useful. On Mac do:
> brew install typesafe-activator

To compile:
> sbt compile

or
> activator compile

To run the specs:
> sbt test

Note: Do not turn off the CONSOLE logging in test/resources/logback.xml as there are Specs that rely on this

To actually run the application, first "assemble" it:
> sbt assembly

This packages up an executable JAR - Note that "assembly" will first compile and test.

Then just run as any executable JAR, with any extra Java options for overriding configurations.

For example, to use a config file (other than the default application.conf) which is located on the file system (in this case in the boot directory)
> java -Dconfig.file=test-classes/my-application.conf -jar <jar name>.jar

Note that the log configuration file could also be included e.g.
> -Dlogback.configurationFile=path/to/my-logback.xml

So a more indepth startup with sbt itself could be:
> sbt run -Dconfig.file=target/scala-2.11/test-classes/application.test.conf -Dlogback.configurationFile=target/scala-2.11/test-classes/logback.test.xml

And to run with SSL:
> sbt -Dconfig.resource=application.ssl.conf run

And other examples:

booting from project root:
> java -Dspray.can.server.port=8080 -jar target/scala-2.11/<jar name>.jar

and running from directory of the executable JAR using a config that is within said JAR:
> java -Dconfig.resource=application.uat.conf -jar <jar name>.jar

Finally you can perform a quick test of the application by calling one of the monitor API e.g. making a cURL call to the application:
> curl http://localhost:9100/mongodb 

SBT - Revolver
--------------
sbt-revolver is a plugin for SBT enabling a super-fast development turnaround for your Scala applications:

See https://github.com/spray/sbt-revolver

For development, you can use ~re-start to go into "triggered restart" mode.
Your application starts up and SBT watches for changes in your source (or resource) files.
If a change is detected SBT recompiles the required classes and sbt-revolver automatically restarts your application. 
When you press &lt;ENTER&gt; SBT leaves "triggered restart" and returns to the normal prompt keeping your application running.

Canned Submission
==================

URL : http://{host}:{port}/{context}/test/global-entry-case
HTTP Method : POST
Content-Type : application/json

Request Body:
=============
```json
{
  "passport": {
    "givenName": "Bruce",
    "familyName": "Wayne",
    "passportNumber": "123456789",
    "expiryDate": "2020-10-10",
    "dateOfBirth": "1980-10-10",
    "gender": "M",
    "nationalityCode": "GBR"
  },
  "previousNames": [
    {
      "givenName": "Clark",
      "familyName": "Kent"
    },
    {
      "givenName": "Peter",
      "familyName": "Parker"
    }
  ],
  "payment": {
    "feeInPence": 4000,
    "orderCode": "GRT000000147"
  },
  "email": "bruce.wayne@gmail.com",
  "referenceId": "GLS6CGQQ"
}
```
Response: 
========
```json
{
    "_id": {
        "$oid": "559fe4673004807e32aa9758"
    },
    "version": 1,
    "status": {},
    "productName": "global-entry",
    "registeredTravellerNumber": "GEE7EE4CJ",
    "applications": [
        {
            "referenceId": "GLS6CGQQ",
            "applicationDate": "2015-07-10T15:27:35Z",
            "passportDetails": {
                "givenName": "Bruce",
                "familyName": "Wayne",
                "passportNumber": "123456789",
                "expiryDate": "2020-10-09T23:00:00Z",
                "dateOfBirth": "1980-10-09T23:00:00Z",
                "gender": "M",
                "passportNationality": "GBR",
                "irisSchemeMembershipNumber": ""
            },
            "paymentDetails": {
                "fee": 40,
                "orderCode": "GRT000000147",
                "paymentDateTime": "2015-07-10T15:27:35Z",
                "canRefund": false,
                "refundDetails": []
            },
            "previousNames": []
        }
    ],
    "currentDetails": {
        "passportDetails": {
            "givenName": "Bruce",
            "familyName": "Wayne",
            "passportNumber": "123456789",
            "expiryDate": "2020-10-09T23:00:00Z",
            "dateOfBirth": "1980-10-09T23:00:00Z",
            "gender": "M",
            "passportNationality": "GBR",
            "irisSchemeMembershipNumber": ""
        },
        "previousNames": [],
        "email": "bruce.wayne@gmail.com"
    },
    "createdDate": "2015-07-10T15:27:35Z",
    "checks": {
        "caseProduct": {},
        "bio": {
            "status": {}
        },
        "duplicate": {
            "status": {}
        },
        "centaur": {
            "status": {}
        },
        "immigration": {
            "status": {},
            "category": {}
        },
        "wi": {
            "status": {}
        },
        "approval": {
            "status": {}
        },
        "reApproval": {
            "status": {}
        },
        "pnc": {
            "status": {}
        },
        "travelHistory": {
            "status": {},
            "resendStatus": {}
        },
        "credibility": {
            "status": {}
        },
        "dailyWash": {
            "status": {},
            "passportLostOrStolen": false
        },
        "greenList": {
            "status": {},
            "awaitingRestore": false,
            "requiresInsert": false,
            "requiresDelete": false
        },
        "suspensionAndExpiry": {
            "status": {},
            "expired": false,
            "suspendedBecausePassportExpired": false,
            "suspendedBecauseVisaExpired": false,
            "manuallySuspended": false,
            "suspendedBecauseLostOrStolenPassport": false
        },
        "ukAccessCodeAcknowledged": {
            "status": {}
        },
        "geApplicationDetails": {
            "status": {}
        }
    },
    "notes": [],
    "legacyCase": false,
    "updatedDate": "2015-07-10T15:27:35Z"
}

Data Load:
==========
If you want to create lots of cases then you can use the data-load project in registered-traveller-caseworker/data-load. You will need to do the following

Start the integration service:
> activator run

Run the global entry data load:
> registered-traveller-caseworker/data-load node geDataLoad.js