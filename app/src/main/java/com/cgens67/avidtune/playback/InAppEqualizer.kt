@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
    UnstableApi::class
)
@file:Suppress("OPT_IN_USAGE", "OPT_IN_USAGE_ERROR")

package com.cgens67.avidtune.playback

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.ui.component.IconButton
import com.cgens67.avidtune.ui.component.SettingsGeneralCategory
import com.cgens67.avidtune.ui.component.SwitchPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.foundation.shape.RoundedCornerShape
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

private val M = Modifier
@Composable private fun sR(id:Int,vararg a:Any)=stringResource(id,*a)
@Composable private fun pR(id:Int)=painterResource(id)
private fun fA(vararg v:Float)=floatArrayOf(*v)

@Serializable enum class FilterType{PK,LSC,HSC,LPQ,HPQ}
@Serializable data class ParametricEQBand(val frequency:Double,val gain:Double,val q:Double=1.41,val filterType:FilterType=FilterType.PK,val enabled:Boolean=true)
@Serializable data class ParametricEQ(val preamp:Double,val bands:List<ParametricEQBand>,val metadata:Map<String,String> = emptyMap())

class BiquadFilter(sr:Int,fq:Double,g:Double,q:Double=1.41,type:FilterType=FilterType.PK){
    private var a1=0.0;private var a2=0.0;private var b0=0.0;private var b1=0.0;private var b2=0.0
    private var x1L=0.0;private var x2L=0.0;private var y1L=0.0;private var y2L=0.0;private var x1R=0.0;private var x2R=0.0;private var y1R=0.0;private var y2R=0.0
    init{val w=2.0*PI*fq/sr;val s=sin(w);val c=cos(w);val a=10.0.pow(g/40.0);val sqA=sqrt(10.0.pow(g/20.0));val al=s/(2.0*q)
        val aP=sqA+1.0;val aM=sqA-1.0;val tA=2.0*sqrt(sqA)*(s/2.0*sqrt((sqA+1.0/sqA)*(0.0)+2.0));var a0=1.0
        when(type){FilterType.PK->{b0=1.0+al*a;b1=-2.0*c;b2=1.0-al*a;a0=1.0+al/a;a1=-2.0*c;a2=1.0-al/a}
            FilterType.LSC->{b0=sqA*(aP-aM*c+tA);b1=2.0*sqA*(aM-aP*c);b2=sqA*(aP-aM*c-tA);a0=aP+aM*c+tA;a1=-2.0*(aM+aP*c);a2=aP+aM*c-tA}
            FilterType.HSC->{b0=sqA*(aP+aM*c+tA);b1=-2.0*sqA*(aM+aP*c);b2=sqA*(aP+aM*c-tA);a0=aP-aM*c+tA;a1=2.0*(aM-aP*c);a2=aP-aM*c-tA}
            else->{b0=1.0+al*a;b1=-2.0*c;b2=1.0-al*a;a0=1.0+al/a;a1=-2.0*c;a2=1.0-al/a}};b0/=a0;b1/=a0;b2/=a0;a1/=a0;a2/=a0}
    fun p(i:Double)=(b0*i+b1*x1L+b2*x2L-a1*y1L-a2*y2L).also{x2L=x1L;x1L=i;y2L=y1L;y1L=it}
    fun pS(l:Double,r:Double)=Pair(p(l),(b0*r+b1*x1R+b2*x2R-a1*y1R-a2*y2R).also{x2R=x1R;x1R=r;y2R=y1R;y1R=it})
    fun rst(){x1L=0.0;x2L=0.0;y1L=0.0;y2L=0.0;x1R=0.0;x2R=0.0;y1R=0.0;y2R=0.0}
}

@UnstableApi class CustomEqualizerAudioProcessor:AudioProcessor{
    private var sr=0;private var ch=0;private var enc=C.ENCODING_INVALID;private var act=false;private var en=false;private var end=false
    private var oB=ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());private var f=emptyList<BiquadFilter>();private var pA=1.0;private var pEq:ParametricEQ?=null
    @Synchronized fun apply(p:ParametricEQ){if(sr==0){pEq=p;return};pA=10.0.pow(p.preamp/20.0);f=p.bands.filter{it.enabled&&it.frequency<sr/2.0}.map{BiquadFilter(sr,it.frequency,it.gain,it.q,it.filterType)};en=true;f.forEach{it.rst()}}
    @Synchronized fun disable(){en=false;f=emptyList();pA=1.0;pEq=null};fun isEnabled()=en
    override fun configure(a:AudioProcessor.AudioFormat):AudioProcessor.AudioFormat{sr=a.sampleRate;ch=a.channelCount;enc=a.encoding;pEq?.let{apply(it);pEq=null};if(enc!=C.ENCODING_PCM_16BIT||ch>2)throw AudioProcessor.UnhandledAudioFormatException(a);act=true;return a}
    override fun isActive()=act;override fun getOutput()=oB.also{oB=ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())}
    override fun queueInput(i:ByteBuffer){val rm=i.remaining();if(rm==0)return;if(oB.capacity()<rm)oB=ByteBuffer.allocateDirect(rm).order(ByteOrder.nativeOrder())else oB.clear();if(!en||f.isEmpty()){oB.put(i).flip();return}
        repeat(rm/2/ch){if(ch==1){var s=i.short.toDouble()/32768.0;f.forEach{s=it.p(s)};oB.putShort(((s*pA)*32768.0).coerceIn(-32768.0,32767.0).toInt().toShort())}else{var l=i.short.toDouble()/32768.0;var r=i.short.toDouble()/32768.0;f.forEach{val(pl,pr)=it.pS(l,r);l=pl;r=pr}
            oB.putShort(((l*pA)*32768.0).coerceIn(-32768.0,32767.0).toInt().toShort());oB.putShort(((r*pA)*32768.0).coerceIn(-32768.0,32767.0).toInt().toShort())}};oB.flip()}
    override fun isEnded()=end&&oB.remaining()==0;override fun queueEndOfStream(){end=true}
    override fun flush(){oB=ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());end=false;f.forEach{it.rst()}}
    override fun reset(){flush();sr=0;ch=0;enc=C.ENCODING_INVALID;act=false}
}

@Singleton class EqualizerService @Inject constructor(){
    private val p=mutableListOf<CustomEqualizerAudioProcessor>();private var pEq:ParametricEQ?=null;private var sD=false
    fun add(x:CustomEqualizerAudioProcessor){p.add(x);if(sD)x.disable()else pEq?.let{x.apply(it)}}
    fun applyProfile(e:ParametricEQ):Result<Unit>{pEq=e;sD=false;return runCatching{p.forEach{it.apply(e)}}}
    fun disable(){sD=true;pEq=null;p.forEach{it.disable()}}
}

@HiltViewModel class AxionEqViewModel @Inject constructor(
    @ApplicationContext c:Context,
    private val s:EqualizerService
):ViewModel(){
    private val p=c.getSharedPreferences("vivi_eq_prefs",0)
    val fQ=doubleArrayOf(31.0,62.0,125.0,250.0,500.0,1000.0,2000.0,4000.0,8000.0,16000.0)
    private val _en=MutableStateFlow(p.getBoolean("enabled",false));val en=_en.asStateFlow()
    private val _bG=MutableStateFlow(FloatArray(10){p.getFloat("band_$it",0f)});val bG=_bG.asStateFlow()
    private val _m=MutableStateFlow(p.getInt("mode",0));val m=_m.asStateFlow()
    private val _d=MutableStateFlow(false);val d=_d.asStateFlow()
    init{if(_en.value)ap()}
    fun setEn(o:Boolean){_en.value=o;p.edit().putBoolean("enabled",o).apply();if(o)ap()else{s.disable()}}
    fun setM(m:Int){_m.value=m;p.edit().putInt("mode",m).apply();_d.value=false}
    fun setG(i:Int,g:Float){val n=_bG.value.copyOf();n[i]=g;_bG.value=n;p.edit().putFloat("band_$i",g).apply();_d.value=true;if(_en.value)ap()}
    fun setGs(g:FloatArray,u:Boolean=false){_bG.value=g;p.edit().apply{g.forEachIndexed{i,v->putFloat("band_$i",v)}}.apply();_d.value=u;if(_en.value)ap()}
    fun rst()=setGs(FloatArray(10){0f})
    private fun ap()=viewModelScope.launch{
        val eq=ParametricEQ(0.0, _bG.value.mapIndexed{i,f->ParametricEQBand(fQ[i],f.toDouble()/50.0,1.41,FilterType.PK,true)})
        s.applyProfile(eq)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable 
fun EqScreen(nav:NavController?=null,vm:AxionEqViewModel=hiltViewModel()){
    AxionEqScreen(bck = { nav?.navigateUp() }, vm = vm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable 
fun AxionEqScreen(bck:()->Unit,vm:AxionEqViewModel=hiltViewModel()){
    val en by vm.en.collectAsState();val bG by vm.bG.collectAsState();val m by vm.m.collectAsState();val cS=MaterialTheme.colorScheme
    val c = LocalContext.current
    val sys=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){}
    val ply=LocalPlayerConnection.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sR(R.string.vivi_equalizer)) },
                navigationIcon = {
                    com.cgens67.avidtune.ui.component.IconButton(
                        onClick = bck,
                        onLongClick = {}
                    ) {
                        Icon(pR(R.drawable.arrow_back), null)
                    }
                },
                actions = {
                    IconButton(onClick={
                        ply?.let{p->
                            val i=Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply{
                                putExtra(AudioEffect.EXTRA_AUDIO_SESSION,p.player.audioSessionId)
                                putExtra(AudioEffect.EXTRA_PACKAGE_NAME,c.packageName)
                                putExtra(AudioEffect.EXTRA_CONTENT_TYPE,0)
                            }
                            if(i.resolveActivity(c.packageManager)!=null)sys.launch(i)
                        }
                    }){Icon(pR(R.drawable.equalizer),null)}
                }
            )
        }
    ) { pd ->
        Column(M.fillMaxSize().verticalScroll(rememberScrollState()).padding(pd).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            SettingsGeneralCategory(
                title = null,
                items = listOf(
                    {
                        SwitchPreference(
                            title = { Text(sR(R.string.eq_enable_title)) },
                            description = sR(R.string.eq_enable_summary),
                            icon = { Icon(pR(R.drawable.equalizer), null) },
                            checked = en,
                            onCheckedChange = { vm.setEn(it) }
                        )
                    }
                )
            )
            Row(M.fillMaxWidth().padding(horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)){
                ToggleButton(checked=m==0,onCheckedChange={vm.setM(0)},modifier=M.weight(1f).semantics{role=Role.RadioButton},shapes=ButtonGroupDefaults.connectedLeadingButtonShapes()){Text(sR(R.string.eq_simple))}
                ToggleButton(checked=m==1,onCheckedChange={vm.setM(1)},modifier=M.weight(1f).semantics{role=Role.RadioButton},shapes=ButtonGroupDefaults.connectedTrailingButtonShapes()){Text(sR(R.string.eq_advanced))}
            }
            AnimatedContent(targetState=m,transitionSpec={fadeIn() togetherWith fadeOut()},label=""){cM->
                if(cM==0){
                    var b by remember{mutableFloatStateOf(0f)};var md by remember{mutableFloatStateOf(0f)};var t by remember{mutableFloatStateOf(0f)};
                    LaunchedEffect(bG){b=bG[1]/50f;md=(bG[4]+bG[5])/2f/50f;t=bG[8]/50f}
                    val aEq={val bv=(b*50f).coerceIn(-600f,600f);val mv=(md*50f).coerceIn(-600f,600f);val tv=(t*50f).coerceIn(-600f,600f);vm.setGs(fA(bv*1.1f,bv,bv*0.7f+mv*0.3f,bv*0.2f+mv*0.8f,mv,mv,mv*0.8f+tv*0.2f,mv*0.3f+tv*0.7f,tv,tv*1.15f),true)}
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)){
                        CircularEqControl(b,md,t,en,{b=it;aEq()},{md=it;aEq()},{t=it;aEq()},M.fillMaxWidth(0.9f).padding(horizontal=8.dp).aspectRatio(1f))
                        
                        listOf(
                            listOf<Pair<Int,FloatArray>>(R.string.eq_preset_flat to fA(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f),R.string.eq_preset_vivi_signature to fA(150f,100f,50f,0f,-20f,0f,80f,150f,200f,150f),R.string.eq_preset_acoustic to fA(150f,150f,50f,75f,100f,75f,125f,175f,150f,75f),R.string.eq_preset_spatial to fA(75f,50f,25f,-50f,-25f,0f,50f,75f,100f,75f)),
                            listOf<Pair<Int,FloatArray>>(R.string.eq_preset_bass_boost to fA(500f,400f,250f,100f,0f,-50f,0f,100f,200f,300f),R.string.eq_preset_pure_clarity to fA(-100f,-50f,0f,50f,150f,250f,300f,250f,150f,100f),R.string.eq_preset_soft_bass to fA(200f,180f,140f,80f,30f,20f,60f,90f,110f,130f),R.string.eq_preset_electronic to fA(350f,280f,120f,-50f,-150f,50f,180f,300f,400f,500f)),
                            listOf<Pair<Int,FloatArray>>(R.string.eq_preset_rock to fA(300f,220f,150f,50f,-100f,120f,200f,250f,320f,380f),R.string.eq_preset_pop to fA(-150f,0f,100f,180f,250f,220f,150f,80f,-50f,-120f),R.string.eq_preset_jazz to fA(150f,100f,60f,140f,200f,180f,120f,180f,220f,200f),R.string.eq_preset_voice to fA(-250f,-150f,0f,200f,400f,380f,200f,120f,0f,-120f))
                        ).forEachIndexed{i,c->PresetSection(if(i==0)sR(R.string.eq_label_vivi)else "",c,null,null,en,bG,{if(en)vm.setGs(it)})}
                        
                        PresetSection(sR(R.string.eq_label_dolby),listOf<Pair<Int,FloatArray>>(R.string.eq_preset_dolby_open to fA(150f,180f,220f,180f,160f,210f,250f,280f,180f,80f),R.string.eq_preset_dolby_rich to fA(100f,160f,200f,220f,280f,260f,240f,200f,150f,50f),R.string.eq_preset_dolby_focused to fA(-300f,-50f,130f,180f,220f,120f,140f,100f,-50f,-300f)),null,null,en,bG,{if(en)vm.setGs(it)})
                        PresetSection(sR(R.string.eq_label_dirac),listOf<Pair<Int,FloatArray>>(R.string.eq_preset_dirac_music to fA(200f,140f,80f,0f,30f,80f,140f,200f,280f,350f),R.string.eq_preset_dirac_movie to fA(300f,250f,150f,0f,70f,120f,180f,250f,320f,400f),R.string.eq_preset_dirac_game to fA(150f,250f,200f,0f,80f,150f,300f,450f,400f,280f)),null,null,en,bG,{if(en)vm.setGs(it)})
                    }
                }else{val ls=arrayOf("31","62","125","250","500","1k","2k","4k","8k","16k")
                    Column(verticalArrangement=Arrangement.spacedBy(16.dp)){
                        Box(M.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(cS.surfaceContainerLow).padding(vertical=16.dp)){
                            Row(M.horizontalScroll(rememberScrollState()).padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                for(b in 0..9){
                                    Column(M.width(56.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                        Text("%.1f".format(bG[b]/10f),style=MaterialTheme.typography.labelSmall,color=if(en)cS.primary else cS.outline);
                                        Spacer(M.height(4.dp));
                                        Box(M.height(200.dp),contentAlignment=Alignment.Center){
                                            Slider(value=bG[b],onValueChange={vm.setG(b,it)},valueRange=-600f..600f,enabled=en,modifier=M.width(200.dp).layout{m,c->val p=m.measure(c.copy(minWidth=c.minHeight,maxWidth=c.maxHeight));layout(p.height,p.width){p.place(-p.width/2+p.height/2,p.width/2-p.height/2)}}.graphicsLayer{rotationZ=-90f})
                                        };
                                        Spacer(M.height(4.dp));
                                        Text(ls[b],style=MaterialTheme.typography.labelSmall,color=cS.onSurfaceVariant)
                                    }
                                }
                            }
                        };
                        Row(M.fillMaxWidth(),horizontalArrangement=Arrangement.Center){
                            OutlinedButton(onClick={vm.rst()}){Icon(Icons.Rounded.Replay,null);Spacer(M.width(8.dp));Text(sR(R.string.eq_reset))}
                        }
                    }
                }
            }
            Spacer(M.height(60.dp))
        }
    }
}

@Composable
private fun PresetSection(
    title: String,
    list: List<Pair<Int, FloatArray>>,
    names: List<String>?,
    onEdit: (() -> Unit)?,
    enabled: Boolean,
    currentGains: FloatArray,
    onSetGains: (FloatArray) -> Unit
) {
    val cS = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = cS.primary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                if (onEdit != null && enabled) {
                    androidx.compose.material3.IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Edit, null, tint = cS.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            list.forEachIndexed { i, (nr, f) ->
                val isSelected = currentGains.size == f.size && currentGains.zip(f).all { abs(it.first - it.second) < 10f }
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { if (enabled) onSetGains(f) },
                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    enabled = enabled,
                    shapes = when {
                        list.size == 1 || i == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        i == list.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                ) {
                    Text(
                        text = names?.getOrNull(i) ?: sR(nr), 
                        style = MaterialTheme.typography.labelSmall, 
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable fun CircularEqControl(ba:Float,mi:Float,tr:Float,en:Boolean,oB:(Float)->Unit,oM:(Float)->Unit,oT:(Float)->Unit,m:Modifier=M){
    val tm=rememberTextMeasurer();val cS=MaterialTheme.colorScheme;val p=cS.primary;val pc=cS.primaryContainer;val o=cS.outline;val os=cS.onSurface;val sc=cS.surfaceContainerLow;val ls=TextStyle(fontSize=11.sp,color=os);val vs=TextStyle(fontSize=13.sp,color=os)
    val lM=sR(R.string.eq_label_mid);val lB=sR(R.string.eq_label_bass);val lT=sR(R.string.eq_label_treble)
    var act by remember{mutableIntStateOf(-1)};val cb by rememberUpdatedState(oB);val cm by rememberUpdatedState(oM);val ct by rememberUpdatedState(oT);val angs=remember{doubleArrayOf(-PI/2,-PI/2+2*PI/3,-PI/2+4*PI/3)}
    Canvas(m.pointerInput(en){if(!en)return@pointerInput;awaitEachGesture{val d=awaitFirstDown();val w=size.width.toFloat();val cx=w/2f;val cy=size.height.toFloat()/2f
        val tA=atan2((d.position.y-cy).toDouble(),(d.position.x-cx).toDouble());var aI=0;var bD=Double.MAX_VALUE;for(i in 0..2){val df=abs(atan2(sin(tA-angs[i]),cos(tA-angs[i])));if(df<bD){bD=df;aI=i}};val ax=if(hypot((d.position.x-cx).toDouble(),(d.position.y-cy).toDouble())<w*0.48f)aI else -1
        if(ax<0)return@awaitEachGesture;d.consume();act=ax;val calc={pos:Offset->(((pos.x-cx)*cos(angs[ax]).toFloat()+(pos.y-cy)*sin(angs[ax]).toFloat())/(w/2f*0.35f)-1f)/0.8f*10f};val dp={v:Float->when(ax){0->cm(v.coerceIn(-10f,10f));1->cb(v.coerceIn(-10f,10f));2->ct(v.coerceIn(-10f,10f))}};dp(calc(d.position))
        while(true){val e=awaitPointerEvent();val c=e.changes.firstOrNull()?:break;if(!c.pressed)break;c.consume();dp(calc(c.position))};act=-1}}){
        val cx=size.width/2f;val cy=size.height/2f;val oR=size.width/2f*0.9f;val bR=size.width/2f*0.35f
        drawCircle(sc,oR,Offset(cx,cy));drawCircle(o.copy(0.25f),oR,Offset(cx,cy),style=Stroke(1.5f));val vA=fA(mi,ba,tr);val lbs=arrayOf(lM,lB,lT)
        for(i in 0..2){val x2=cx+oR*0.88f*cos(angs[i]).toFloat();val y2=cy+oR*0.88f*sin(angs[i]).toFloat();drawLine(o.copy(0.2f),Offset(cx,cy),Offset(x2,y2),strokeWidth=1f);for(d in 1..7)drawCircle(o.copy(0.5f),3.5f,Offset(cx+bR*(0.3f+d*0.2f)*cos(angs[i]).toFloat(),cy+bR*(0.3f+d*0.2f)*sin(angs[i]).toFloat()))}
        drawCircle(o.copy(0.1f),bR,Offset(cx,cy),style=Stroke(0.8f));val wp=Path()
        for(s in 0..72){val t=s/72f;val sA=t*2.0*PI-PI/2;var r=bR;for(i in 0..2){val df=sA-angs[i];val wr=atan2(sin(df),cos(df));val w=cos(wr*0.75).toFloat().coerceAtLeast(0f);r+=bR*(vA[i]/10f).coerceIn(-1f,1f)*0.8f*w*w};val px=cx+r*cos(sA).toFloat();val py=cy+r*sin(sA).toFloat();if(s==0)wp.moveTo(px,py)else wp.lineTo(px,py)}
        wp.close();drawPath(wp,pc.copy(0.12f));drawPath(wp,p.copy(0.4f),style=Stroke(2f,cap=StrokeCap.Round))
        for(i in 0..2){val r=bR*(1f+(vA[i]/10f).coerceIn(-1f,1f)*0.8f);val pt=Offset(cx+r*cos(angs[i]).toFloat(),cy+r*sin(angs[i]).toFloat());val iA=act==i;drawCircle(p.copy(if(iA)0.2f else 0.1f),if(iA)32f else 26f,pt);drawCircle(if(iA)p else os,if(iA)18f else 14f,pt)
            val m1=tm.measure(lbs[i],ls);val m2=tm.measure("${if(vA[i]>=0)"+" else ""}${vA[i].toInt()}",vs);val lR=oR*0.78f;val lx=cx+lR*cos(angs[i]).toFloat();val ly=cy+lR*sin(angs[i]).toFloat();drawText(m1,os.copy(0.6f),Offset(lx-m1.size.width/2f,ly-m1.size.height-2f));drawText(m2,os,Offset(lx-m2.size.width/2f,ly+2f))}
    }
}
