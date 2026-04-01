package com.example.expencetracker.mapper;


import com.example.expencetracker.dto.*;
import com.example.expencetracker.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/**
* MapStruct mapper — converts between the domain model and DTOs.
*
* How MapStruct works:
*   At compile time, the @Mapper annotation triggers the MapStruct annotation
*   processor which generates a concrete implementation class
*   (ExpenseMapperImpl.java) in the target/ directory.
*   We never write that class ourselves — the processor does it.
*
* componentModel = "spring":
*   Registers the generated impl as a Spring @Component so we can
*   @Autowired / constructor-inject it normally.
*/
@Mapper(componentModel = "spring")
public interface ExpenseMapper {

   /**
    * Converts a client's Request DTO → domain Expense model.
    *
    * Note: `id`, `createdAt`, and `updatedAt` are intentionally NOT mapped here
    * because they are set by the database / service layer, not by the client.
    */
   Expense toModel(ExpenseDTO.Request request);

   /**
    * Converts a domain Expense model → Response DTO for sending to the client.
    * All fields are mapped by matching name + type.
    */
   ExpenseDTO.Response toResponse(Expense expense);

   /**
    * Applies fields from the Request DTO onto an EXISTING Expense model in-place.
    * Used during PUT updates so we don't need to re-fetch the full object.
    *
    * @MappingTarget tells MapStruct to update the target object rather than
    * create a new one.
    */
   void updateModel(ExpenseDTO.Request request, @MappingTarget Expense expense);
}
