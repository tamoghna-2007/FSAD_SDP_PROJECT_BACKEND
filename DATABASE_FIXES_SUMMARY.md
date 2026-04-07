# Database Constraint Violation Fixes - Summary

## Issues Identified and Fixed

### 1. **Nullable Foreign Key Constraints**
**Problem:** All foreign key relationships were marked as `nullable = false`, which meant that dependent entities MUST have a parent entity. This caused 409 Conflict errors when trying to create entities independently.

**Example:**
- User required a Group
- Task required a Group
- Group required a Project
- Submission required a Project

**Solution:** Changed all foreign key `@JoinColumn` annotations from `nullable = false` to `nullable = true`

**Files Modified:**
- `User.java`: `group_id` now nullable
- `Group.java`: `project_id` now nullable
- `Task.java`: `group_id` and `assigned_to_user_id` now nullable
- `Submission.java`: `project_id` now nullable

---

### 2. **Infinite Recursion in JSON Serialization**
**Problem:** When entities with bidirectional relationships were serialized to JSON, it caused infinite recursion:
- Project → Groups → Users → Group (back to parent, causing loop)
- Group → Users/Tasks → Group (circular reference)

**Solution:** Added `@JsonIgnore` annotation to all child collections and foreign key references:
```java
@OneToMany(mappedBy = "project")
@JsonIgnore
private List<Group> groups;

@ManyToOne
@JsonIgnore
private Group group;
```

**Files Modified:**
- `User.java`: Added @JsonIgnore to `group` field
- `Project.java`: Added @JsonIgnore to `groups` and `submissions` collections
- `Group.java`: Added @JsonIgnore to `project`, `users`, and `tasks`
- `Task.java`: Added @JsonIgnore to `group` and `assignedTo`
- `Submission.java`: Added @JsonIgnore to `project`

---

### 3. **Aggressive Cascade Settings**
**Problem:** Changed cascade type from `CascadeType.ALL` with `orphanRemoval = true` to `CascadeType.PERSIST`

**Why:**
- `CascadeType.ALL` cascades DELETE operations, which can cause unintended data loss
- `orphanRemoval = true` deletes child records when removed from parent collection
- For development phase, we want independence - safer to use only PERSIST

**Solution:** Updated all OneToMany relationships to use safer cascade settings

**Files Modified:**
- `Project.java`: Groups and Submissions now use `CascadeType.PERSIST`
- `Group.java`: Users and Tasks now use `CascadeType.PERSIST`

---

### 4. **DTO Implementation for Better API Design**
**Problem:** 
- GroupController and SubmissionController were accepting raw entity objects
- This forced clients to send nested JSON objects, causing validation issues
- No separation between API contract and database model

**Solution:** 
- Created `GroupDto.java` with `projectId` instead of full Project object
- Created `SubmissionDto.java` with `projectId` instead of full Project object
- Updated controllers to properly map between DTOs and entities with null-safe operations

**Files Created:**
- `GroupDto.java`
- `SubmissionDto.java`

**Files Modified:**
- `GroupController.java`: Now uses GroupDto with proper null-safe project lookup
- `SubmissionController.java`: Now uses SubmissionDto with proper null-safe project lookup

---

### 5. **Controllers Updated for Optional Relationships**
All controllers now properly handle optional foreign keys:

**Pattern Used:**
```java
private Task toEntity(TaskDto taskDto) {
    Group group = null;
    if (taskDto.getGroupId() != null) {
        group = groupRepository.findById(taskDto.getGroupId())
            .orElseThrow(() -> new ResourceNotFoundException(...));
    }
    
    User assignedUser = null;
    if (taskDto.getAssignedToUserId() != null) {
        assignedUser = userRepository.findById(taskDto.getAssignedToUserId())
            .orElseThrow(() -> new ResourceNotFoundException(...));
    }
    
    return Task.builder()
        .id(taskDto.getId())
        .title(taskDto.getTitle())
        .group(group)
        .assignedTo(assignedUser)
        .build();
}
```

---

## Summary of Changes by Entity

### User Entity
```
BEFORE:
@JoinColumn(name = "group_id", nullable = false)
private Group group;

AFTER:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "group_id", nullable = true)
@JsonIgnore
private Group group;
```

### Project Entity
```
BEFORE:
@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Group> groups;

AFTER:
@OneToMany(mappedBy = "project", cascade = CascadeType.PERSIST)
@JsonIgnore
private List<Group> groups;
```

### Group Entity
```
BEFORE:
@JoinColumn(name = "project_id", nullable = false)
private Project project;

AFTER:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "project_id", nullable = true)
@JsonIgnore
private Project project;
```

### Task Entity
```
BEFORE:
@JoinColumn(name = "group_id", nullable = false)
private Group group;

AFTER:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "group_id", nullable = true)
@JsonIgnore
private Group group;
```

### Submission Entity
```
BEFORE:
@JoinColumn(name = "project_id", nullable = false)
private Project project;

AFTER:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "project_id", nullable = true)
@JsonIgnore
private Project project;
```

---

## API Usage Examples (POST)

### Create a Project (No Dependencies)
```json
POST /api/projects
{
  "title": "Project Alpha",
  "description": "First project",
  "deadline": "2024-12-31"
}
```

### Create a Group (Optional Project)
```json
POST /api/groups
{
  "groupName": "Group A"
}
// or with projectId
{
  "groupName": "Group A",
  "projectId": 1
}
```

### Create a User (Optional Group)
```json
POST /api/users
{
  "name": "John Doe",
  "email": "john@example.com",
  "role": "Student"
}
// or with groupId
{
  "name": "John Doe",
  "email": "john@example.com",
  "role": "Student",
  "groupId": 1
}
```

### Create a Task (Optional Group and User)
```json
POST /api/tasks
{
  "title": "Task 1",
  "description": "Do something",
  "status": "PENDING"
}
```

### Create a Submission (Optional Project)
```json
POST /api/submissions
{
  "fileUrl": "s3://bucket/file.pdf",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## Key Improvements

✅ **Independent Entity Creation** - All entities can be created without cascading dependencies  
✅ **No Circular References** - JSON serialization won't cause infinite loops  
✅ **Safer Cascade Operations** - PERSIST only, prevents accidental deletes  
✅ **Flexible Relationships** - Optional foreign keys allow gradual data entry  
✅ **Better API Design** - DTOs provide clear API contracts  
✅ **Null-Safe Operations** - Controllers handle missing references gracefully  

---

## Testing Recommendations

1. **Test Independent Creation:**
   - Create Project → Create Group → Create User → Create Task

2. **Test Serialization:**
   - GET /api/projects - Should not have circular dependencies

3. **Test Optional Relationships:**
   - Create User without groupId
   - Create Task without assignedToUserId
   - Create Group without projectId

4. **Test Cascading:**
   - Create Group linked to Project
   - Delete Project - Group should remain (not orphaned)

