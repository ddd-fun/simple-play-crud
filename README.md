TDD way of tackling CRUD with Play, Scala and Scala-Check.

Iteration 1 - “Walking skeleton” Branch rest-api.

My first step was creation of “Walking skeleton”. The purpose of the walking skeleton is to deliver minimal possible solution which has a business value. Meantime, it also helps to get better understanding of the requirements and technologies.  Having created play project, I started from writing a failing test which run against real application. Why failing test? Well, for maintainability sake it's always a good idea to explore how test reports failures. Then I made it green by implementing functionality. I gradually added missing api endpoints by following this way. Please, have a look on controllers.Application and AppSkeletonSpec. I didn’t implement neither validation nor clean domain model yet, but I delivered business value and I established “test safety net” which enables me to do further improvements.

Iteration 2. Validation and domain modeling. Branch domain-modeling

The first challenge was to test my validations rules. Normally, it’s not a good idea to check validation rules against running server. It is overkill which makes test cycle slow and increase release time. Thus, I needed a unit test solution. I deliberately started my unit testing in imperative style (please, see DetailedAppSpec), just to show why declarative “scala check”-based solution is better choice. Indeed, it’s time consuming and boring exercise   to write all possible valid/invalid scenarios, instead I described what valid/invalid data is, and let the framework to generate test cases, please see AppValidationSpec and DomainDataGen.  Then I tested state in the same declarative way by describing commands and state transitions (please see AppStateSpec) and again letting scala-check to go over all possible transition chains.
The next challenges were business logic which still had been resided in controller and evident duplication of the following workflow pattern: {IF advert is found THEN do something (update/delete etc), ELSE interrupt}, so it’s was a trigger for exploring domain algebra and make it “composable”. Please, have a look at AdvertService where I used Option as monadic composition unit. If you are interested in functional domain modeling, please feel free to explore garage-guru-fun, where I used wide range of monads, free-monad and monad transformers.


Iteration 2. Persistence.  Not implemented yet.

Integrate with DynamoDB. 
