# Testing

> [!NOTE]
> Evaluators NOTE:
>
> Due to time constraint, only a sub-set of the tests were completed.
> 
> The purpose is to demonstrate the ideas outlined by the test standards in this document

## Test Standards 

- Unit Test at the Domain Layer 
  - Ensure that business behaviour is correct, agnostic to the application-specific technology implementation. - See [Example](../src/test/java/fucai/me/makanplace/domain/model/MakanSessionTest.java)
   ```java
   @Test        
   void suggestPlaceFreshSuggestion() {
       // Given: Participant has enrolled in the session
       // When: Participant suggests a place  for the session
       // Then: The suggested place will be logged
       final MakanKaki kaki = MakanKaki.builder()
               .id("kaki_id1")
               .displayName("John Wick")
               .build();
   
       MakanSession session = templateBuilder().build()
               .enroll(kaki)
               .suggestPlace(kaki.getId(), "McDonalds");
   
       assertEquals(session.getParticipants().size(), 1);
       assertEquals(session.getParticipants().get(kaki.getId()), kaki);
   
       assertTrue(session.getSelectedPlace().isEmpty());
       final MakanPlace expectedMakanPlace =
               MakanPlace.builder().suggesters(ImmutableMap.of(kaki.getId(), kaki)).placeName("McDonalds").build();
       assertEquals(1, session.getSuggestedPlaces().size());
       assertEquals(expectedMakanPlace, session.getSuggestedPlaces().get("McDonalds"));
   }  
   ```
- All documented functional requirements must be tallied against within the test code for the Domain Services. For [example](../src/test/java/fucai/me/makanplace/domain/service/CreateMakanSessionServiceTest.java) :
   ```java
      @Test
      public void testCreateFromCleanSlate() {
          /***                            
           * R1: A user can initiate a session and invite others to join it.
           * AC1 : A new session is created from scratch without any errors.
           *
           * Given: A user is not enrolled in another session
           * When: The user attempts to create a new session.
           *
           * The User provides:
           * Display Name of the Session
           * Display Name of the User
           * Date Time to meet
           * Then: A new makan session is created:
           *
           **/

          // Given user is not enrolled in any other session
          Mockito.when(makanSessionRepository.findByOwnerIdAndStateIsActive(Mockito.anyString())).thenReturn(null);
          Mockito.when(makanSessionRepository.findByMakanKakiIdAndStateIsActive(Mockito.anyString())).thenReturn(null);

          MakanKaki owner = MakanKaki.builder()
                  .id("owner_id")
                  .displayName("Owner Id")
                  .build();
          MakanSession sessionToCreate = makanSessionTemplate()
                  .owner(owner).build();

          service.createMakanSession(sessionToCreate);

          Mockito.verify(makanSessionRepository).save(sessionToCreate);
      }
    ```
## Integration Test

To verify that the session based mechanics are working, an End-To-End Spring Boot Integration test is written.
See [Here](../src/test/java/fucai/me/makanplace/integration/tests/MakanSessionSmokeTestIT.java)