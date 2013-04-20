package msi.gama.database.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import msi.gama.common.util.GuiUtils;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.GamaList;

public class MSSQLConnection extends SqlConnection{
	private static final boolean DEBUG = false; // Change DEBUG = false for release version
	private static final String WKT2GEO="geometry::STGeomFromText";
	
	@Override
	public Connection connectDB() throws ClassNotFoundException,
			InstantiationException, SQLException, IllegalAccessException {
		// TODO Auto-generated method stub
		Connection conn = null;
		try {
			if ( vender.equalsIgnoreCase(MSSQL) ) {
				Class.forName(MSSQLDriver).newInstance();
				conn =
					DriverManager.getConnection("jdbc:sqlserver://" + url + ":" + port 
						+ ";databaseName=" + dbName + ";user=" + userName + ";password=" + password 
						+ ";");
			}  else {
				throw new ClassNotFoundException("MSSQLConnection.connectDB: The " 
			                                       + vender
			                                       + " is not supported!");
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ClassNotFoundException(e.toString());
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new InstantiationException(e.toString());
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalAccessException(e.toString());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SQLException(e.toString());
		}
		return conn;

	}

	@Override
	protected GamaList<GamaList<Object>> resultSet2GamaList(ResultSetMetaData rsmd, ResultSet rs) 
	{
		// TODO Auto-generated method stub
		// convert Geometry in SQL to Geometry type in GeoTool	
		GamaList<GamaList<Object>> repRequest = new GamaList<GamaList<Object>>();
		try{
			List<Integer>  geoColumn = getGeometryColumns(rsmd);
			int nbCol = rsmd.getColumnCount();
			int i=1;
			if (DEBUG) GuiUtils.debug("Number of col:"+ nbCol);
			if (DEBUG) GuiUtils.debug("Number of row:"+ rs.getFetchSize());
			while (rs.next())
			{
				//InputStream inputStream = rs.getBinaryStream(i);
				if (DEBUG) GuiUtils.debug("processing at row:"+ i);

				GamaList<Object> rowList = new GamaList<Object>();
				for ( int j = 1; j <= nbCol; j++ ) 
				{
					// check column is geometry column?
					if (DEBUG) GuiUtils.debug("col "+j+": " +rs.getObject(j));
					if (geoColumn.contains(j)){
						if (DEBUG) GuiUtils.debug("convert at ["+ i+","+j+"]: ");
							rowList.add(SqlUtils.read(rs.getBytes(j)));
					}
					else
						rowList.add(rs.getObject(j));
				}
				repRequest.add(rowList);
				i++;
			}
			if (DEBUG) GuiUtils.debug("Number of row:"+ i);
		}
		catch (Exception e)
		{
			
		}
		return repRequest;
					
	}

	@Override
	protected List<Integer> getGeometryColumns(ResultSetMetaData rsmd)
			throws SQLException 
	{
		// TODO Auto-generated method stub
		 int numberOfColumns = rsmd.getColumnCount();
		 List<Integer>  geoColumn = new ArrayList<Integer>();
		 for (int i=1; i<=numberOfColumns; i++){
			 
			 if (DEBUG){
				 GuiUtils.debug("col "+ i + ": "+ rsmd.getColumnName(i));
				 GuiUtils.debug("   - Type: "+ rsmd.getColumnType(i));
				 GuiUtils.debug("   - TypeName: "+ rsmd.getColumnTypeName(i));
				 GuiUtils.debug("  - size: "+ rsmd.getColumnDisplaySize(i));
				 
			 }
			
			 /* for Geometry 
			 - in MySQL Type: -2/-4   - TypeName: UNKNOWN   - size: 2147483647
			 - In MSSQL Type: -3   - TypeName: geometry     - size: 2147483647
			 - In SQLITE Type: 2004   - TypeName: BLOB      - size: 2147483647
			 - In PostGIS/PostGresSQL Type: 1111   - TypeName: geometry  - size: 2147483647
			          st_asbinary(geom):   - Type: -2   - TypeName: bytea  - size: 2147483647
			 */
			 // Search column with Geometry type
			 if (vender.equalsIgnoreCase(MSSQL) & rsmd.getColumnType(i)==-3 ) 
					 geoColumn.add(i);
		}		
		 return geoColumn;

	}

	@Override
	protected GamaList<Object> getColumnTypeName(ResultSetMetaData rsmd) throws SQLException 
	{
		// TODO Auto-generated method stub
		 int numberOfColumns = rsmd.getColumnCount();
		 GamaList<Object>  columnType = new GamaList<Object>();
		 for (int i=1; i<=numberOfColumns; i++){
			 /* for Geometry 
			 - in MySQL Type: -2/-4   - TypeName: UNKNOWN   - size: 2147483647
			 - In MSSQL Type: -3   - TypeName: geometry     - size: 2147483647
			 - In SQLITE Type: 2004   - TypeName: BLOB      - size: 2147483647
			 - In PostGIS/PostGresSQL Type: 1111   - TypeName: geometry  - size: 2147483647
			 */
		     // Search column with Geometry type
			 if (vender.equalsIgnoreCase(MSSQL) & rsmd.getColumnType(i)==-3 ) 
				 columnType.add(GEOMETRYTYPE);
			 else
				 columnType.add(rsmd.getColumnTypeName(i).toUpperCase());
		}		
		return columnType;				

	}

	@Override
	protected String getInsertString(IScope scope, Connection conn,
			String table_name, GamaList<Object> cols, GamaList<Object> values)
			throws GamaRuntimeException
	{
		// TODO Auto-generated method stub
		int col_no=cols.size();
		String insertStr= "INSERT INTO ";
		String selectStr="SELECT ";
		String colStr="";
		String valueStr="";
		//Check size of parameters
		if (values.size()!=col_no) {
			throw new IndexOutOfBoundsException("Size of columns list and values list are not equal");
		}
		//Get column name
		for (int i=0; i<col_no;i++){
			if (i==col_no-1){
				colStr= colStr+ (String) (cols.get(i));
			}else{
				colStr= colStr+ (String) (cols.get(i))+",";
			}					
		}
		// create SELECT statement string
		selectStr=selectStr + " TOP 1 "+ colStr + " FROM " + table_name +" ;";		

		if (DEBUG){
			GuiUtils.debug("MSSQLConnection.getInsertString.select command:"+selectStr);
		}
		
		try{
			//get column type;
			Statement st = conn.createStatement(); 
			ResultSet rs = st.executeQuery(selectStr);
			ResultSetMetaData rsmd = rs.getMetaData();
			GamaList<Object> col_Names= getColumnName(rsmd);
			GamaList<Object> col_Types=getColumnTypeName(rsmd);
			
			if (DEBUG){
				GuiUtils.debug("list of column Name:" + col_Names);
				GuiUtils.debug("list of column type:" + col_Types);
			}
			//Insert command
			//set parameter value
			valueStr="";
			for (int i=0; i<col_no;i++){
				//Value list begin-------------------------------------------
				if (((String)col_Types.get(i)).equalsIgnoreCase(GEOMETRYTYPE)){ // for GEOMETRY type
					    //Transform GAMA GIS TO NORMAL
						if (transformed){
							WKTReader wkt = new WKTReader();
							Geometry geo2 =scope.getWorldScope().getGisUtils().inverseTransform(wkt.read(values.get(i).toString()));
							valueStr=valueStr+WKT2GEO+"('"+geo2.toString()+"')";
						}else{
							valueStr=valueStr+WKT2GEO+"('"+values.get(i).toString()+"')";
						}
				}else if (((String)col_Types.get(i)).equalsIgnoreCase(CHAR)
						||((String)col_Types.get(i)).equalsIgnoreCase(VARCHAR)
						||((String)col_Types.get(i)).equalsIgnoreCase(NVARCHAR)
						||((String)col_Types.get(i)).equalsIgnoreCase(TEXT)){ // for String type
					//Correct error string
					String temp=values.get(i).toString();
					temp=temp.replaceAll("'", "''");
					//Add to value:
					valueStr=valueStr+"'"+temp+"'";
				}else { // For other type
					valueStr=valueStr+values.get(i).toString();
				}
				if (i!=col_no-1){ // Add delimiter of each value 
					valueStr=valueStr + ",";
				}
				//Value list end--------------------------------------------------------

			}									
			insertStr= insertStr + table_name +"("+colStr+") " +"VALUES("+valueStr+")";					

			if(DEBUG) 
				GuiUtils.debug("MSSQLConnection.getInsertString:" +insertStr );
			
			
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new GamaRuntimeException("MSSQLConnection.getInsertString " + e.toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new GamaRuntimeException("MSSQLConnection.getInsertString " + e.toString());
		}
		
		return insertStr;
	}

	@Override
	protected String getInsertString(IScope scope, Connection conn,
			String table_name, GamaList<Object> values)
			throws GamaRuntimeException {
		// TODO Auto-generated method stub
		String insertStr= "INSERT INTO ";
		String selectStr="SELECT ";
		String colStr="";
		String valueStr="";

		//Get column name
		// create SELECT statement string
			selectStr=selectStr + " TOP 1 * " + " FROM " + table_name +" ;";
		try{
			//get column type;
			Statement st = conn.createStatement(); 
			ResultSet rs = st.executeQuery(selectStr);
			ResultSetMetaData rsmd = rs.getMetaData();
			GamaList<Object> col_Names= getColumnName(rsmd);
			GamaList<Object> col_Types=getColumnTypeName(rsmd);
			int col_no=col_Names.size();
			//Check size of parameters
			if (values.size()!=col_Names.size()) {
				throw new IndexOutOfBoundsException("Size of columns list and values list are not equal");
			}

			//Insert command
			//set parameter value
			colStr="";
			valueStr="";
			for (int i=0; i<col_no;i++){
				//Value list begin-------------------------------------------
				if (((String)col_Types.get(i)).equalsIgnoreCase(GEOMETRYTYPE)){ // for GEOMETRY type
					    //Transform GAMA GIS TO NORMAL
						if (transformed){
							WKTReader wkt = new WKTReader();
							Geometry geo2 =scope.getWorldScope().getGisUtils().inverseTransform(wkt.read(values.get(i).toString()));
							valueStr=valueStr+WKT2GEO+"('"+geo2.toString()+"')";
						}else{ 
							valueStr=valueStr+WKT2GEO+"('"+values.get(i).toString()+"')";
						}
				}else if (((String)col_Types.get(i)).equalsIgnoreCase(CHAR)
							||((String)col_Types.get(i)).equalsIgnoreCase(VARCHAR)
							||((String)col_Types.get(i)).equalsIgnoreCase(NVARCHAR)
							||((String)col_Types.get(i)).equalsIgnoreCase(TEXT))
				{ // for String type
						//Correct error string
						String temp=values.get(i).toString();
						temp=temp.replaceAll("'", "''");
						//Add to value:
						valueStr=valueStr+"'"+temp+"'";
				}else { // For other type
						valueStr=valueStr+values.get(i).toString();
				}
				//Value list end--------------------------------------------------------
				//column list
				colStr=colStr + col_Names.get(i).toString();

				if (i!=col_no-1){ // Add delimiter of each value 
					colStr=colStr + ",";
					valueStr=valueStr + ",";
				}
			}
					
			insertStr= insertStr + table_name +"("+colStr+") " +"VALUES("+valueStr+")";					

			if(DEBUG) 
				GuiUtils.debug("SqlConection.getInsertString:" +insertStr );
			
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new GamaRuntimeException("MSSQLConnection.insertBD " + e.toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new GamaRuntimeException("MSSQLConnection.insertBD " + e.toString());
		}
		
		return insertStr;
	}
}