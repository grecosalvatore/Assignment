package it.polito.dp2.RNS.sol3.service.resources;


import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.*;
import it.polito.dp2.RNS.sol3.service.RnsService.RnsDeployer;
import it.polito.dp2.RNS.sol3.service.RnsService.RnsService;

import java.net.URI;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.datatype.XMLGregorianCalendar;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/rns")
@Api(value = "/rns")
public class RnsResources {
	public UriInfo uriInfo;
	
	RnsService service = new RnsService();
	RnsDeployer rnsDeployer = new RnsDeployer();
	
	public RnsResources(@Context UriInfo uriInfo) {
		this.uriInfo = uriInfo;
	}
	
	@GET
    @ApiOperation(value = "getRns", notes = "reads main resource with link on principals reasources"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=Rns.class),
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Rns getRns() {
		return service.getRns(uriInfo);
	}
	
	@GET
	@Path("/vehicles")
    @ApiOperation(value = "getVehicles", notes = "Read the set of all vehicles in the system")
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=Vehicles.class),
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Vehicles getVehicles() {
		return service.getvehicles(uriInfo);
	}

	@GET
	@Path("/vehicles/{id}")
    @ApiOperation(value = "getVehicle", notes = "Read single vehicle"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=Vehicle.class),
    		@ApiResponse(code = 404, message = "Not Found - vehicle is not in the system"),
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Response getVehicle(@PathParam("id") String plateId) {
		return service.getVehicle(uriInfo,plateId);
	}
	
	@PUT
	@Path("/vehicles/{id}")
    @ApiOperation(value = "enterVehicleRequest", notes = "Vehicle request permission to enter the system"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "Permession granted",response=SuggestedPath.class),
    		@ApiResponse(code = 400, message = "Bad Request - origin or destiation are not in the system or validation error"),
    		@ApiResponse(code = 403, message = "Forbidden - Origin is not an IN/OUT gate or vehicel is already present "),
    		})
	@Consumes({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Response enterVehicleRequest(@PathParam("id") String id, Vehicle vehicle) {
		if(id.equals(vehicle.getPlateId())){
			return service.tryEnterVehicle(uriInfo, vehicle);
		}else{
			//plateId != id of resource
			return Response.status(Response.Status.BAD_REQUEST).entity("Plate id and resource uri are different").build();
		}
	}
	
	@DELETE
	@Path("/vehicles/{id}")
    @ApiOperation(value = "deleteVehicle", notes = "Remove a vehicle from the system (for administrators)"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK - Vehicle succesfully removed from the system"),
    		@ApiResponse(code = 404, message = "Not Found - vehicle is not in the system"),
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Response removeVehicle(@PathParam("id") String plateId) {
		return service.deleteVehicle(plateId);
	}
	

	
	@GET
	@Path("/vehicles/{id}/state")
    @ApiOperation(value = "getState", notes = "Read the current state for the vehicle"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=State.class),
    		@ApiResponse(code = 404, message = "Not Found - vehicle is not in the system"),
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Response getState(@PathParam("id") String plateId) {
		return service.getVehicleState(uriInfo, plateId);
	}
	
	@PUT
	@Path("/vehicles/{id}/state")
    @ApiOperation(value = "changeState", notes = "The vehicle request to change the current state"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK - vehicle has succesfully changed the state",response=State.class),
    		@ApiResponse(code = 400, message = "Bad Request - body validation error"),
    		@ApiResponse(code = 404, message = "Not Found - vehicle is not in the system"),
    		})
	@Consumes({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Response changeState(@PathParam("id") String id, State newState) {
		return service.changeVehicleState(uriInfo, id, newState);
	}

	@GET
	@Path("/vehicles/{id}/suggestedPath")
    @ApiOperation(value = "getSuggestedPath", notes = "Read the suggested path for the vehicle"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=SuggestedPath.class),
    		@ApiResponse(code = 404, message = "Not Found - vehicle is not in the system"),
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Response getSuggestedPath(@PathParam("id") String plateId) {	
		return service.getSuggestedPath(uriInfo,plateId);
	}

	@GET
	@Path("/vehicles/{id}/currentPosition")
    @ApiOperation(value = "getCurrentPosition", notes = "Read the current position for the vehicle"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=Position.class),
    		@ApiResponse(code = 404, message = "Not found - vehicle is not in the system"),
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Response getCurrentPosition(@PathParam("id") String plateId) {
		return service.getVehicleCurrentPosition(uriInfo,plateId);
	}
	
	@PUT
	@Path("/vehicles/{id}/currentPosition")
    @ApiOperation(value = "changeCurrentPosition", notes = "Vehicle request to change the current position "
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK - Vehicle succesfully changed the position",response=SuggestedPath.class),
    		@ApiResponse(code = 400, message = "Bad Request - Valildation error or new position is not in the system"),
    		@ApiResponse(code = 403, message = "Forbidden - New position is not reachable from current position"),
    		@ApiResponse(code = 404, message = "Not Found - vehicle is not in the system"),
    		})
	@Consumes({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Response changeCurrentPosition(@PathParam("id") String plateId, Position newPosition) {
		return service.changeCurrentPosition(uriInfo, plateId, newPosition);
	}
	
	@DELETE
	@Path("/vehicles/{id}/currentPosition")
    @ApiOperation(value = "exitVehicleRequest", notes = "Vehicle request to exit the system from this current position(for vehicle clients)"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK - vehicle successfully exited the system"),
    		@ApiResponse(code = 403, message = "Forbidden - current position is not OUT/INOUT gate"),
    		@ApiResponse(code = 404, message = "Not Found - vehicle is not in the system"),
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Response exitVehicleRequest(@PathParam("id") String plateId) {
		return service.exitVehicleRequest(plateId);
	}

	@GET
	@Path("/places")
    @ApiOperation(value = "getPlaces", notes = "Read the set of all places in the system"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=Places.class),
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Places getPlaces() {
		return service.getPlaces(uriInfo);
	}
	
	@GET
	@Path("/places/{id}")
    @ApiOperation(value = "getPlace", notes = "Read single place"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=Place.class),
    		@ApiResponse(code = 404, message = "Not Found - place is not in the system"),
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Response getPlace(@PathParam("id") String placeId) {
		return service.getPlace(uriInfo,placeId);
	}
	
	@GET
	@Path("/places/{id}/currentVehicles")
    @ApiOperation(value = "getCurrentVehicles", notes = "Read all vehicles inside the selected place"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=Vehicles.class),
    		@ApiResponse(code = 404, message = "Not Found - place is not in the system"),
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Vehicles getCurrentVehicles(@PathParam("id") String placeId) {
		return service.getCurrentVehiclesByPlaceId(uriInfo, placeId);
	}
	
	@GET
	@Path("/places/{id}/connectedTo")
    @ApiOperation(value = "getNextPlaces", notes = "Read all places reachable from this place"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=Places.class),
    		@ApiResponse(code = 404, message = "Not Found - place is not in the system"),
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public ConnectedTo getNextPlaces() {
		return null;
	}
	
	@GET
	@Path("/places/gates")
    @ApiOperation(value = "getGates", notes = "Read the set of all gates in the system"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=Places.class),  		
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Places getGates() {
		return service.getGates(uriInfo);
	}
	
	@GET
	@Path("/places/roadSegments")
    @ApiOperation(value = "getRoadSegments", notes = "Read the set of all road segments in the system"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=Places.class),  		
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Places getRoadSegments() {
		return service.getRoadSegments(uriInfo);
	}
	
	@GET
	@Path("/places/parkingAreas")
    @ApiOperation(value = "getParkingAreas", notes = "Read the set of all parking areas in the system"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=Places.class),  		
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Places getParkingAreas() {
		return service.getParkingAreas(uriInfo);
	}
	
	@GET
	@Path("/connections")
    @ApiOperation(value = "getConnections", notes = "Read the set of all connections in the system"
	)
    @ApiResponses(value = {
    		@ApiResponse(code = 200, message = "OK",response=Connections.class),  		
    		})
	@Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
	public Connections getConnections() {
		return service.getConnections(uriInfo);
	}
	
	
}
