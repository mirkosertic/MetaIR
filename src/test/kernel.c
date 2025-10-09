__kernel void BytecoderKernel(__global float4* val$theInputs, __global int* val$theMostSimilar, __global float* val$theMostSimilarity) {
    int var0;
    int var1;
    __global float4* var2;
    float4 var3;
    float var4;
    float phi5;
    int phi6;
    int phi7;
    float4 var8;
    float var9;
    float var10;
    float var11;
    int var12;
    float phi13;
    int phi14;
    var0 = get_global_id(0);
    var1 = get_global_size(0);
    var2 = val$theInputs;
    var3 = var2[var0];
    var4 = length(var3);
    phi5 = -1.0;
    phi6 = -1;
    phi7 = 0;
    L2092801316: while(true) {
     if (phi7 >= var1) {
      val$theMostSimilar[var0] = phi6;
      val$theMostSimilarity[var0] = phi5;
      return;
     } else {
      If_31_0: {
       if (phi7 == var0) {
        phi13 = phi5;
        phi14 = phi6;
        goto If_31_0_exit;
       } else {
        var8 = val$theInputs[phi7];
        var9 = (var4 * length(var8));
        if ((var9 > 0.0 ? 1  : (var9 < 0.0 ? -1 : 0)) == 0) {
         phi13 = phi5;
         phi14 = phi6;
         goto If_31_0_exit;
        } else {
         var10 = dot(var3,var8);
         var11 = (var10 / var9);
         if (var11 <= phi5) {
          phi13 = phi5;
          phi14 = phi6;
          goto If_31_0_exit;
         } else {
          var12 = phi7;
          phi13 = var11;
          phi14 = var12;
          goto If_31_0_exit;
         }
        }
       }
      }
      If_31_0_exit:
      phi7 = (phi7 + 1);
      phi5 = phi13;
      phi6 = phi14;
      goto L2092801316;
     }
    }
    }
