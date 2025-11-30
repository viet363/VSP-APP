import com.example.app.Model.ProductSpecificationModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface SpecificationApi {

    @GET("products/{id}/specification")
    fun getSpecification(@Path("id") productId: Long): Call<List<ProductSpecificationModel>>

}
