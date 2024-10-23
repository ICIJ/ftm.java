# ftm.java

This repository is providing a Java (or JVM based) classes and records from the [Follow The Money](https://followthemoney.tech) models.

The goal is to use the YAML source files to generate Java records and classes, then publish a jar that contains the generated models.

The schema files are [here](https://github.com/alephdata/followthemoney/tree/main/followthemoney/schema).

This is a work in progress, for now, only "required" fields are generated in Java code.

To use it, just make `mvn package` it will generate and compile FtM models in a jar.

```mermaid
classDiagram
direction BT
class Address
class Airplane
class Analyzable {
<<Interface>>

}
class Article
class Assessment
class Asset
class Associate
class Audio
class BankAccount
class Call {
<<Interface>>

}
class CallForTenders
class Company
class Contract
class ContractAward
class CourtCase
class CourtCaseParty
class CryptoWallet
class Debt
class Directorship
class Document
class Documentation
class EconomicActivity {
<<Interface>>

}
class Email
class Employment
class Event
class Family
class Folder
class HyperText
class Identification
class Image
class Interest {
<<Interface>>

}
class Interval {
<<Interface>>

}
class LegalEntity
class License
class Membership
class Mention
class Message
class Note
class Occupancy
class Organization
class Ownership
class Package
class Page {
<<Interface>>

}
class Pages
class Passport
class Payment
class Person
class PlainText
class Position
class Post
class Project
class ProjectParticipant {
<<Interface>>

}
class PublicBody
class RealEstate
class Representation
class Sanction
class Security
class Similar {
<<Interface>>

}
class Succession
class Table
class TaxRoll
class Thing
class Trip
class UnknownLink
class UserAccount
class Value {
<<Interface>>

}
class Vehicle
class Vessel
class Video
class Workbook

Address  -->  Thing 
Airplane  -->  Thing 
Airplane  ..>  Vehicle 
Article  -->  Document 
Assessment  -->  Thing 
Asset  -->  Thing 
Asset  ..>  Value 
Associate  ..>  Interval 
Audio  -->  Document 
BankAccount  ..>  Asset 
BankAccount  -->  Thing 
CallForTenders  ..>  Interval 
CallForTenders  -->  Thing 
Company  ..>  Asset 
Company  -->  Organization 
Contract  ..>  Asset 
Contract  -->  Thing 
ContractAward  ..>  Interest 
ContractAward  ..>  Value 
CourtCase  -->  Thing 
CourtCaseParty  ..>  Interest 
CryptoWallet  -->  Thing 
CryptoWallet  ..>  Value 
Debt  ..>  Interval 
Debt  ..>  Value 
Directorship  ..>  Interest 
Document  ..>  Analyzable 
Document  -->  Thing 
Documentation  ..>  Interest 
Email  -->  Document 
Email  ..>  Folder 
Email  ..>  HyperText 
Email  ..>  PlainText 
Employment  ..>  Interest 
Event  ..>  Analyzable 
Event  ..>  Interval 
Event  -->  Thing 
Family  ..>  Interval 
Folder  -->  Document 
HyperText  -->  Document 
Identification  ..>  Interval 
Image  -->  Document 
LegalEntity  -->  Thing 
License  -->  Contract 
Membership  ..>  Interest 
Message  -->  Document 
Message  ..>  Folder 
Message  ..>  HyperText 
Message  ..>  Interval 
Message  ..>  PlainText 
Note  ..>  Analyzable 
Note  -->  Thing 
Occupancy  ..>  Interval 
Organization  -->  LegalEntity 
Ownership  ..>  Interest 
Package  -->  Document 
Package  ..>  Folder 
Pages  -->  Document 
Passport  -->  Identification 
Payment  ..>  Interval 
Payment  ..>  Value 
Person  -->  LegalEntity 
PlainText  -->  Document 
Position  -->  Thing 
Post  ..>  Interest 
Project  ..>  Interval 
Project  -->  Thing 
Project  ..>  Value 
PublicBody  -->  Organization 
RealEstate  ..>  Asset 
RealEstate  -->  Thing 
Representation  ..>  Interest 
Sanction  ..>  Interval 
Security  ..>  Asset 
Security  -->  Thing 
Succession  ..>  Interest 
Table  -->  Document 
TaxRoll  ..>  Interval 
Trip  -->  Event 
UnknownLink  ..>  Interest 
UserAccount  -->  Thing 
Vehicle  ..>  Asset 
Vehicle  -->  Thing 
Vessel  -->  Thing 
Vessel  ..>  Vehicle 
Video  -->  Document 
Workbook  -->  Document 
Workbook  ..>  Folder 

```

