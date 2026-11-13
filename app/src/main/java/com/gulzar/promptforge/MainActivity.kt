package com.gulzar.promptforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity:ComponentActivity(){ override fun onCreate(b:Bundle?){super.onCreate(b);setContent{MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF8A6D00),secondary=Color(0xFFFFD700))){App(SecureStore(this))}}} }

class MainVm:ViewModel(){ var config by mutableStateOf(AppConfig()); var prompt by mutableStateOf(""); var generation by mutableStateOf<Generation?>(null); var status by mutableStateOf("Ready"); var busy by mutableStateOf(false) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(store:SecureStore, vm:MainVm=viewModel()) {
    val scope=rememberCoroutineScope(); var tab by remember{mutableIntStateOf(0)}
    LaunchedEffect(Unit){vm.config=store.load()}
    Scaffold(topBar={TopAppBar(title={Text("PromptForge")})}){pad->Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()){
        TabRow(tab){listOf("Build","GitHub setup").forEachIndexed{i,t->Tab(tab==i,{tab=i},text={Text(t)})}}
        Spacer(Modifier.height(12.dp)); if(tab==1) Settings(vm){store.save(vm.config);vm.status="Settings saved"} else Builder(vm,scope)
    }}
}
@Composable fun Settings(vm:MainVm,onSave:()->Unit){Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Text("Keys are encrypted with Android Keystore and stay on this device.",style=MaterialTheme.typography.bodySmall)
    Field("OpenRouter API key",vm.config.openRouterKey,{vm.config=vm.config.copy(openRouterKey=it)},true); Field("GitHub fine-grained token",vm.config.githubToken,{vm.config=vm.config.copy(githubToken=it)},true)
    Field("GitHub owner",vm.config.owner,{vm.config=vm.config.copy(owner=it)});Field("Repository",vm.config.repo,{vm.config=vm.config.copy(repo=it)});Field("Branch",vm.config.branch,{vm.config=vm.config.copy(branch=it)});Field("Free model fallback list",vm.config.preferredModels,{vm.config=vm.config.copy(preferredModels=it)})
    Button(onClick=onSave,modifier=Modifier.fillMaxWidth()){Text("Save securely")}; Button(onClick={vm.busy=true; Thread{runCatching{Api.testGithub(vm.config)}.onSuccess{vm.status="Connected to $it"}.onFailure{vm.status=it.message?:"Connection failed"};vm.busy=false}.start()},modifier=Modifier.fillMaxWidth()){Text("Test GitHub connection")};Text(vm.status)
}}
@Composable fun Field(label:String,value:String,on:(String)->Unit,password:Boolean=false){OutlinedTextField(value,on,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=if(password)PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)}
@Composable fun Builder(vm:MainVm,scope:kotlinx.coroutines.CoroutineScope){Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
    OutlinedTextField(vm.prompt,{vm.prompt=it},label={Text("Describe the app or change")},modifier=Modifier.fillMaxWidth().height(150.dp))
    Button(enabled=!vm.busy && vm.prompt.isNotBlank(),onClick={scope.launch{vm.busy=true;vm.status="Trying free AI models...";vm.generation=runCatching{withContext(Dispatchers.IO){Api.generate(vm.config,vm.prompt)}}.onFailure{vm.status=it.message?:"Generation failed"}.getOrNull();if(vm.generation!=null)vm.status="Review the files before applying";vm.busy=false}},modifier=Modifier.fillMaxWidth()){Text("Generate code")}; if(vm.busy)LinearProgressIndicator(Modifier.fillMaxWidth());Text(vm.status)
    vm.generation?.let{g->Text(g.summary,style=MaterialTheme.typography.titleMedium);g.files.forEachIndexed{i,f->Row(Modifier.fillMaxWidth()){Checkbox(f.selected,{v->vm.generation=g.copy(files=g.files.toMutableList().also{it[i]=f.copy(selected=v)})});Column{Text(f.path);Text("${f.content.length} characters",style=MaterialTheme.typography.bodySmall)}}};Button(enabled=!vm.busy && g.files.any{it.selected},onClick={scope.launch{vm.busy=true;vm.status="Applying selected files...";val r=runCatching{withContext(Dispatchers.IO){g.files.filter{it.selected}.forEach{Api.pushFile(vm.config,it,"PromptForge: ${g.summary.take(60)}")}}};vm.status=r.fold({"Changes committed to ${vm.config.owner}/${vm.config.repo}"},{it.message?:"GitHub update failed"});vm.busy=false}},modifier=Modifier.fillMaxWidth()){Text("Review complete: apply to GitHub")}}
}}
