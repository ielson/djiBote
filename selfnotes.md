Program running order:

onCreate                                                                                      
Permissions answered                                                                          
connectionActivity set                                                                        
Handler started                                                                               
Product Change                                                                                
on Notify Status Change                                                                       
Registered to DJISDK                                                                          
Product Change                                                                                
BaseProductListener set                                                                       
on Notify Status Change                                                                       
Connect Button Pressed         
------ alterando activity para main ----                                                               
MainActivity Started                            
onCreate after rosjava activity                 
onInitUI                                        
buttons found                                   
onClickListeners for mTakeOff, mLan and mStickBt
onResume                                        
onInitPreviewer                                 
Previewer Init                                  
init Flight Controller                          
Talker created                                  
got product flight controller                   
setting flight controller modes                 
Flight Controller Init                          
onProductChange                                 
VideoStreamDecoder resumed                      
onPause                                         
onStop                                                                           
--- apos atividade do ROS ----                                                      
On Init Method                                                                                
onResume                                                                                      
onInitPreviewer                                                                               
Previewer Init                                                                                
init Flight Controller                                                                        
Talker created                                                                                
got product flight controller                                                                 
setting flight controller modes                                                               
Flight Controller Init                                                                        
onProductChange                                                                               
VideoStreamDecoder resumed                                                                    
node configuration done, with this parameters: org.ros.node.NodeConfiguration@6669077         
3 nodes executed                                                  
--- drone desconctado, alterando activity para connect ----                            
Connectivity Changed                                                                          
on Notify Status Change                                                                       
Component Change                                                                              
on Notify Status Change                                                                       
Component Change                                                                              
on Notify Status Change                                                                       
Component Change                                                                              
on Notify Status Change                                                                       
Component Change                                                                              
on Notify Status Change        

dji api at an older version
https://web.archive.org/web/20170615025625/http://developer.dji.com/api-reference/android-api/BaseClasses/DJIBaseProduct.html#djibaseproduct_baseproductlistener_inline

comment sem espaco = comentei nesse branch //ALO ou */ALO

erro na linha 194 (mFlightController.setStateCallback(new FlightControllerState.Callback(){)
init Flight Controller 
Flight Controller Init

nao ta pegando o flightController do product
Attempt to invoke virtual method 'boolean dji.sdk.base.BaseProduct.isConnected()' on a null object reference

private void initPreviewer() {
// faz a camera aparecer na tela do cel
Log.d("FLOW main", "onInitPreviewer");
product = ConnectionActivity.mProduct;