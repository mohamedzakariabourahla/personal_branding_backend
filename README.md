```text
├───application
│   └───service
│           UserService.java
│           
├───domain
│   ├───model
│   │       User.java
│   │       
│   └───repository
│           UserRepository.java
│           
├───infrastructure
│   ├───config
│   │       BeanConfig.java
│   │       
│   └───persistence
│       ├───entity
│       │       UserEntity.java
│       │       
│       ├───jpa
│       │       JpaUserRepository.java
│       │       
│       └───repositoryAdapter
│               UserRepositoryAdapter.java
│               
└───presentation
    ├───controller
    │       UserController.java
    │       
    └───dto
        ├───request
        │       UserRequest.java
        │       
        └───response
                UserResponse.java
