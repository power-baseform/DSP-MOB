
/*
 Baseform
 Copyright (C) 2018  Baseform

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

#import "PowerCityController.h"
#import <WebKit/WebKit.h>
#import <QuartzCore/QuartzCore.h>

#define TOKEN_KEY @"TOKEN"

#define UIColorFromRGB(rgbValue) [UIColor colorWithRed:((float)((rgbValue & 0xFF0000) >> 16))/255.0 green:((float)((rgbValue & 0xFF00) >> 8))/255.0 blue:((float)(rgbValue & 0xFF))/255.0 alpha:1.0]

#import <CoreNFC/CoreNFC.h>
#import <CoreNFC/NFCNDEFReaderSession.h>

@interface PowerCityController ()
    <UIImagePickerControllerDelegate,
        WKNavigationDelegate,UITabBarDelegate,
        WKUIDelegate, UITableViewDataSource, UITableViewDelegate,
        NFCNDEFReaderSessionDelegate>
@property(weak) IBOutlet WKWebView * webView;
@property(strong) NSString * type;
@property (weak, nonatomic) IBOutlet UIView   * wrapper;
@property (weak, nonatomic) IBOutlet UIView   * topBar;
@property (weak, nonatomic) IBOutlet UITabBar * tabBar;
@property (weak, nonatomic) IBOutlet UIView   * powerTop;
@property (weak, nonatomic) IBOutlet UIView   * scWrapper;
@property (weak, nonatomic) IBOutlet UITableView * pickChallengeTV;
@property (weak, nonatomic) IBOutlet UIView  * commentWrapper;
@property (weak, nonatomic) IBOutlet UIButton *challengeButton;
@property (weak, nonatomic) IBOutlet UITextField *commentTitle;
@property (weak, nonatomic) IBOutlet UITextView *commentText;
@property (weak, nonatomic) IBOutlet UIButton *sendComment;
@property (weak, nonatomic) IBOutlet UIImageView *uploadedImg;
@property (weak, nonatomic) IBOutlet UIActivityIndicatorView *loadingSpinner;
@property (weak, nonatomic) IBOutlet UIView *scanWrapper;
@property (weak, nonatomic) IBOutlet UIButton *scanButton;
@property (strong) NSDictionary * formImage;
@property(strong) NSURLSessionDataTask * task;
@property(strong) NSString * httpUsername;
@property(strong) NSString * httpPassword;
@property(strong) NSString * rootURLPath;
@property(strong) NSString * tabPath;
@property(strong) NSArray * challenges;
@property(readwrite) NSString * challengePk;
@property(strong) NSString * jsessionId;
@property (nonatomic, strong) NSObject *session;
//@property (nonatomic, strong) NFCNDEFReaderSession *alert;
@property(nonatomic,strong) NSString * challengeNFC;
@property (nonatomic,readwrite) dispatch_queue_t nfc_dispatch_queue ;
@property(nonatomic,readwrite) BOOL nfcAvailable;
@end


@implementation PowerCityController

- (BOOL)hasNFC {
    for (NSBundle *bundle in NSBundle.allFrameworks) {
        if ([bundle classNamed:@"NFCNDEFReaderSession"]) {
            return YES;
        }
    }
    return NO;
}

-(void)resetSubmitForm{
    self.formImage = nil;
    [self.challengeButton setTitle:@"Pick challenge" forState:UIControlStateNormal];
    self.commentTitle.text = @"";
    self.commentText.text = @"";
    [self.uploadedImg setHidden:TRUE];
    [self.sendComment setEnabled:FALSE];
    [self.commentTitle setEnabled:FALSE];
    [self.commentText setEditable:FALSE];
    self.challengeNFC = nil;
}

-(void)enablePhotoButton:(BOOL)flag{
    [[self.tabBar.items objectAtIndex:3] setEnabled:flag];
}


- (IBAction)takePicture:(id)sender {
    UIImagePickerController *imagePicker = [[UIImagePickerController alloc] init];
    
    [imagePicker setDelegate:self];
    
    if ([UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera])
        [imagePicker setSourceType:UIImagePickerControllerSourceTypeCamera];
    else // The device doesn't have a camera, so use something like the photos album
        [imagePicker setSourceType:UIImagePickerControllerSourceTypeSavedPhotosAlbum];
    
    [self presentViewController:imagePicker animated:YES completion:^{
        
        
    }];
}

- (IBAction)sendCommentAct:(id)sender {
    NSString * title = [self.commentTitle text];
    NSString * text = [self.commentText text];
    NSString * pk = self.challengePk;
    UIImage * image = nil;
    
    if([title length] == 0)
    {
        UIAlertController * alert = [UIAlertController alertControllerWithTitle: NSLocalizedString(@"ERROR",@"") message:NSLocalizedString(@"EMPTY_TITLE",@"") preferredStyle:UIAlertControllerStyleAlert];
        
        UIAlertAction* action = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK",@"") style:UIAlertActionStyleDefault
                                                       handler:^(UIAlertAction * action) {
                                                            [alert dismissViewControllerAnimated:TRUE completion:^{}];
                                                       }];
        
        [alert addAction:action];
        
        [self presentViewController:alert animated:TRUE completion:^{
            
        }];
        
        return;
    }
    
    if([text length] == 0)
    {
        UIAlertController * alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"ERROR",@"") message:NSLocalizedString(@"EMPTY_BODY",@"") preferredStyle:UIAlertControllerStyleAlert];
        
        UIAlertAction* action = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK",@"") style:UIAlertActionStyleDefault
                                                       handler:^(UIAlertAction * action) {
                                                           [alert dismissViewControllerAnimated:TRUE completion:^{}];
                                                       }];
        
        [alert addAction:action];
        
        [self presentViewController:alert animated:TRUE completion:^{
            
        }];
        
        return;
    }
    
    if(self.formImage != nil){
        UIImage * img = (UIImage*)[self.formImage objectForKey:UIImagePickerControllerOriginalImage];
        if(img!=nil){
            image = img;
        }
    }
    
    NSString *boundary = @"------POWER_3dPSv4_APP";
    
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    
    [request setTimeoutInterval:60];
    [request setHTTPMethod:@"POST"];
    
    [request setURL:[NSURL URLWithString:[NSString stringWithFormat:@"%@%@", self.rootURLPath, @"api.jsp?api=comment"]]];

    NSString *contentType = [NSString stringWithFormat:@"multipart/form-data; boundary=%@", boundary];
    [request setValue:contentType forHTTPHeaderField: @"Content-Type"];
    [request setValue:[NSString stringWithFormat:@"JSESSIONID=%@",self.jsessionId] forHTTPHeaderField: @"Cookie"];
    [request setValue:[self getToken] forHTTPHeaderField:@"tok"];
    
    NSMutableDictionary *parameters = [[NSMutableDictionary alloc] init];
    
    [parameters setValue:title              forKey:@"title"];
    [parameters setValue:text               forKey:@"body"];
    [parameters setValue:pk                 forKey:@"challenge"];
    [parameters setValue:@"image/jpeg"      forKey:@"mime"];
    
    if(self.challengeNFC != nil){
        [parameters setValue:self.challengeNFC forKey:@"nfc"];
    }
    
    NSMutableData *body = [NSMutableData data];
    
    for (NSString *param in parameters) {
        [body appendData:[[NSString stringWithFormat:@"--%@\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];
        [body appendData:[[NSString stringWithFormat:@"Content-Disposition: form-data; name=\"%@\"\r\n\r\n", param] dataUsingEncoding:NSUTF8StringEncoding]];
        [body appendData:[[NSString stringWithFormat:@"%@\r\n", [parameters objectForKey:param]] dataUsingEncoding:NSUTF8StringEncoding]];
    }
    
    if(image!=nil){
        NSString *FileParamConstant = @"photo";
        
        NSData *imageData = UIImageJPEGRepresentation(image, 1);
        
        if (imageData)
        {   
            [body appendData:[[NSString stringWithFormat:@"--%@\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];
            [body appendData:[[NSString stringWithFormat:@"Content-Disposition: form-data; name=\"%@\"; filename=\"image.jpg\"\r\n", FileParamConstant] dataUsingEncoding:NSUTF8StringEncoding]];
            [body appendData:[@"Content-Type:image/jpeg\r\n\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
            [body appendData:imageData];
            [body appendData:[[NSString stringWithFormat:@"\r\n"] dataUsingEncoding:NSUTF8StringEncoding]];
        }
    }
    
    [body appendData:[[NSString stringWithFormat:@"--%@--\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];
    
    [request setHTTPBody:body];

    self.task = [[NSURLSession sharedSession] dataTaskWithRequest:request completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
         NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *) response;
        
        NSInteger statusCode = [httpResponse statusCode];
        
        dispatch_async(dispatch_get_main_queue(),^{
            if(statusCode == 200)
            {
                UIAlertController * alert = [UIAlertController alertControllerWithTitle: NSLocalizedString(@"SUCCESS",@"") message: NSLocalizedString(@"COMMENT_SUBMITTED",@"") preferredStyle:UIAlertControllerStyleAlert];
                
                UIAlertAction* action = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK",@"") style:UIAlertActionStyleDefault
                                                               handler:^(UIAlertAction * action) {
                                                                   [alert dismissViewControllerAnimated:TRUE completion:^{}];
                                                               }];
                [alert addAction:action];
                [self presentViewController:alert animated:TRUE completion:^{ }];
          
                [self.scWrapper setHidden:TRUE];
                self.tabPath = @"?location=home&mobileReq=true";
                [self loadURL:[self pageURL]];
            }
            else{
                UIAlertController * alert = [UIAlertController alertControllerWithTitle: NSLocalizedString(@"ERROR",@"") message: NSLocalizedString(@"UNABLE_TO_COMMENT",@"") preferredStyle:UIAlertControllerStyleAlert];
                
                UIAlertAction* action = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK",@"") style:UIAlertActionStyleDefault
                                                               handler:^(UIAlertAction * action) {
                                                                   [alert dismissViewControllerAnimated:TRUE completion:^{}];
                                                               }];
                
                [alert addAction:action];
                
                [self presentViewController:alert animated:TRUE completion:^{
                    
                }];
            }
        });
    }];
    
    [self.task resume];
    
}

- (IBAction)openChallenges:(id)sender {
    [self.commentWrapper setHidden:TRUE];
    [self.pickChallengeTV setHidden:FALSE];
}

-(NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section{
    return self.challenges != nil ? [self.challenges count] : 0;
}

-(NSInteger)numberOfSectionsInTableView:(UITableView *)tableView{
    return 1;
}

-(UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath{
    NSString * cellId = @"cell123";
    
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:cellId];
    
    if (cell == nil){
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:cellId];
        cell.textLabel.lineBreakMode = UILineBreakModeWordWrap;
        cell.textLabel.numberOfLines = 0;
        cell.textLabel.font = [UIFont fontWithName:@"Helvetica" size:17.0];
    }

    NSInteger count = indexPath.row;
    
    NSDictionary * d = [self.challenges objectAtIndex:count];
    cell.textLabel.text = [d objectForKey:@"title"] ;
    
    return cell;
}

-(void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath{
    NSInteger count = indexPath.row;
    
    NSDictionary * d = [self.challenges objectAtIndex:count];
    
    [self.challengeButton setTitle:[d objectForKey:@"title"] forState:UIControlStateNormal];
    self.challengePk = [NSString stringWithFormat:@"%@",[d objectForKey:@"id"]];
    
    [self.pickChallengeTV setHidden:TRUE];
    [self.commentWrapper setHidden:FALSE];
    
    [self.commentTitle setEnabled:TRUE];
    [self.commentText setEditable:TRUE];
    [self.sendComment setEnabled:TRUE];
}

-(void)setPageType:(NSString*)type{
    self.type = type;
}

-(void)viewDidLoad {
    [super viewDidLoad];
    
    UIWindow * window = UIApplication.sharedApplication.keyWindow;
    CGFloat topPadding = window.safeAreaInsets.top;
    CGFloat bottomPadding = window.safeAreaInsets.bottom;
    
    // Note: Some values are hardcoded because InterfaceBuilder constraints
    // are buggy.
    float tabBarHeight = 60+bottomPadding;
    
    float upperMargin = topPadding == 0 ? 20 : topPadding;
    
    const float width = self.view.bounds.size.width;
    
    self.wrapper.frame = self.view.bounds;
    
    float ptH = 30 + upperMargin;
    self.powerTop.frame = CGRectMake(0, 0, width, ptH);
    
    float tbH = 0;//self.topBar.frame.size.height;
//    self.topBar.frame = CGRectMake(0, ptH, width, tbH);

    float wvH = ptH+tbH;
    self.webView.frame = CGRectMake(0, ptH+tbH,
                                    width,
                                    self.view.frame.size.height - wvH - tabBarHeight);
    
    self.tabBar.frame = CGRectMake(0,self.wrapper.frame.size.height -tabBarHeight,self.wrapper.frame.size.width,tabBarHeight);
    
    self.tabBar.selectedItem = [self.tabBar.items objectAtIndex:0];
    self.tabBar.delegate = self;
    
    self.scWrapper.frame = CGRectMake(0, ptH, width ,
                                      self.view.frame.size.height - ptH  - tabBarHeight);
    
    UITabBarItem *tabBarItem1 = [self.tabBar.items objectAtIndex:0];
    UITabBarItem *tabBarItem2 = [self.tabBar.items objectAtIndex:1];
    UITabBarItem *tabBarItem3 = [self.tabBar.items objectAtIndex:2];
    UITabBarItem *tabBarItem4 = [self.tabBar.items objectAtIndex:3];
    
    tabBarItem1.image = [[UIImage imageNamed:@"issuesIcon.png"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    tabBarItem1.selectedImage = [[UIImage imageNamed:@"issuesIcon.on.png"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    
    tabBarItem2.image = [[UIImage imageNamed:@"aboutIcon.png"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    tabBarItem2.selectedImage = [[UIImage imageNamed:@"aboutIcon.on.png"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    
    tabBarItem3.image = [[UIImage imageNamed:@"accountIcon.png"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    tabBarItem3.selectedImage = [[UIImage imageNamed:@"accountIcon.on.png"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
   
    tabBarItem4.image = [[UIImage imageNamed:@"photoIcon.png"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    tabBarItem4.selectedImage = [[UIImage imageNamed:@"photoIcon.on.png"] imageWithRenderingMode:UIImageRenderingModeAlwaysOriginal];
    
    self.commentTitle.layer.masksToBounds=YES;
    self.commentTitle.layer.borderColor= [UIColorFromRGB(0x3366CC) CGColor];
    self.commentTitle.layer.borderWidth= 1.0f;
    
    self.commentText.layer.masksToBounds=YES;
    self.commentText.layer.borderColor= [UIColorFromRGB(0x3366CC) CGColor];
    self.commentText.layer.borderWidth= 1.0f;
    
    UIView * paddingView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 5, self.commentTitle.frame.size.height)];
    self.commentTitle.leftView = paddingView;
    self.commentTitle.leftViewMode = UITextFieldViewModeAlways;
    
    UITapGestureRecognizer *singleFingerTap = [[UITapGestureRecognizer alloc] initWithTarget:self
                                            action:@selector(handleTopTouch:)];
    [self.powerTop addGestureRecognizer:singleFingerTap];
    

    
    if([self.type isEqualToString:@"sabadel"]){
        self.rootURLPath = @"https://sabadell.power-h2020.eu/";
    }
    else if([self.type isEqualToString:@"milton"]){
        self.rootURLPath = @"https://milton-keynes.power-h2020.eu/";
    }
    else if([self.type isEqualToString:@"jerusalem"]){
        self.rootURLPath = @"https://jerusalem.power-h2020.eu/";
    }
    else if([self.type isEqualToString:@"leicester"]){
        self.rootURLPath = @"https://leicester.power-h2020.eu/";
    }
//    else if([self.type isEqualToString:@"power"]){
//        self.rootURLPath = @"http://city.power-h2020.eu/";
//    }
    
    self.webView.navigationDelegate = self;
    self.webView.UIDelegate = self;
    
    self.tabPath = @"?location=home&mobileReq=true";
    [self loadURL:[self pageURL]];
    
    self.pickChallengeTV.dataSource = self;
    self.pickChallengeTV.delegate = self;
    
    NSString * urlAsString = [NSString stringWithFormat:@"%@%@",
                             self.rootURLPath,@"api.jsp?api=challenges"];
    
    NSCharacterSet *set = [NSCharacterSet URLQueryAllowedCharacterSet];
    NSString *encodedUrlAsString = [urlAsString stringByAddingPercentEncodingWithAllowedCharacters:set];
    
    NSURLSession *session = [NSURLSession sessionWithConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];

    self.task = [session dataTaskWithURL:[NSURL URLWithString:encodedUrlAsString]
            completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
                if (!error)
                {
                    // Success
                    if ([response isKindOfClass:[NSHTTPURLResponse class]])
                    {
                        NSError *jsonError;
                        NSObject *jsonResponse = [NSJSONSerialization JSONObjectWithData:data options:0 error:&jsonError];
                        
                        if (jsonError) {
                            
                        }
                        else if([[jsonResponse class] isSubclassOfClass:[NSArray class]]){
                            self.challenges = (NSArray*)jsonResponse;
                            dispatch_async(dispatch_get_main_queue(),^{
                                [self.pickChallengeTV reloadData];
                                   [self.challengeButton setTitle:
                                    [NSString stringWithFormat:
                                     NSLocalizedString(@"AVAILABLE_CHALLENGES",@"")
                                     ,[self.challenges count]] forState:UIControlStateNormal];
                            });
                        }
                    }
                    else {
                        //Web server is returning an error
                    }
                } else {
                    // Fail
                    NSLog(@"error : %@", error.description);
                }
                
            }];
    
    [self.task resume];
    
    [self enablePhotoButton:[self getToken] != nil];

    self.nfc_dispatch_queue = dispatch_queue_create(NULL,DISPATCH_QUEUE_CONCURRENT);
    
    dispatch_async(self.nfc_dispatch_queue, ^{
        @try{
            BOOL available = [self hasNFC] && [NFCNDEFReaderSession readingAvailable];
            self.nfcAvailable = available;
        }@catch(NSException * e){
            
        }
    });
    
}

//The event handling method
- (void)handleTopTouch:(UITapGestureRecognizer *)recognizer
{
//    CGPoint location = [recognizer locationInView:[recognizer.view superview]];
    [self dismissViewControllerAnimated:TRUE completion:^{}];
}


- (void)tabBar:(UITabBar *)tabBar didSelectItem:(UITabBarItem *)item{
    if(item.tag == 0){
        [self.scWrapper setHidden:TRUE];
        self.tabPath = @"?location=home&mobileReq=true";
        [self loadURL:[self pageURL]];
    }
    else if(item.tag == 1){
        [self.scWrapper setHidden:TRUE];
        self.tabPath = @"?location=about&mobileReq=true";
        [self loadURL:[self pageURL]];
    }
    else if(item.tag == 2){
        [self.scWrapper setHidden:TRUE];
        self.tabPath = @"?location=area&mobileReq=true";
        [self loadURL:[self pageURL]];
    }
    else if(item.tag == 3){
        [self resetSubmitForm];
        [self.scWrapper setHidden:FALSE];
    }
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

-(IBAction)dismiss:(id)sender{
    [self dismissViewControllerAnimated:TRUE completion:^{}];
}


- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary<NSString *,id> *)info{
    [self.uploadedImg setHidden:FALSE];
    self.formImage = info;
    [self dismissViewControllerAnimated:TRUE completion:^{}];
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker{
    [self.uploadedImg setHidden:TRUE];
    self.formImage = nil;
    [self dismissViewControllerAnimated:TRUE completion:^{}];
}

//- (void)locationManager:(CLLocationManager *)manager
//     didUpdateLocations:(NSArray<CLLocation *> *)locations API_AVAILABLE(ios(6.0), macos(10.9)){
//}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/


- (void)webView:(WKWebView *)webView decidePolicyForNavigationResponse:(WKNavigationResponse *)navigationResponse decisionHandler:(void (^)(WKNavigationResponsePolicy))decisionHandler {
    
    NSURLResponse * r = navigationResponse.response;
    
    if([r isKindOfClass:[NSHTTPURLResponse class]]){
        NSHTTPURLResponse * rHTTP = (NSHTTPURLResponse*)r;
        NSDictionary * dic = [rHTTP allHeaderFields];
        
        if([dic objectForKey:@"tok"]){
            NSString * loginToken = [dic objectForKey:@"tok"];
            [self saveToken:loginToken];
            [self enablePhotoButton:TRUE];
        }
    }
    
    decisionHandler(WKNavigationResponsePolicyAllow);
}

- (void)webView:(WKWebView *)webView didFinishNavigation:(null_unspecified WKNavigation *)navigation{
    [webView evaluateJavaScript:@"var oldWindowOpen = window.open;\
     window.open = function(url, sName, sFeatures, bReplace) {\
     oldWindowOpen(url, '_self');\
     };" completionHandler:^(id cenas, NSError * error) {
         
     }];
    
    [[[WKWebsiteDataStore defaultDataStore] httpCookieStore] getAllCookies:^(NSArray<NSHTTPCookie *> * _Nonnull data) {
        for(NSHTTPCookie * c in data){
            if([[c name] isEqualToString:@"JSESSIONID"]){
                NSString * value = [c value];
                self.jsessionId = value;
            }
        }
    }];
    
    [self.loadingSpinner setHidden:TRUE];
}

- (void)webView:(WKWebView *)webView didStartProvisionalNavigation:(null_unspecified WKNavigation *)navigation{
    NSString * urlStr = [webView.URL absoluteString];
    
    // Apagar token de login.
    if([urlStr containsString:@"logout"]){
        [self logout];
    }
    
    NSURL * pageURL = webView.URL;
    
    if([[pageURL host] isEqualToString: [[NSURL URLWithString:self.rootURLPath] host]]){
        [self.loadingSpinner setHidden:FALSE];
    }
    
}

- (void)webView:(WKWebView *)webView decidePolicyForNavigationAction:(WKNavigationAction *)navigationAction
decisionHandler:(void (^)(WKNavigationActionPolicy))decisionHandler{
    NSURL * baseURL = [NSURL URLWithString:self.rootURLPath];
    NSURL * pageURL = webView.URL;
    NSURL * actionURL = navigationAction.request.URL;

    if(![[pageURL host] isEqualToString: [baseURL host]])
    {
        if(!([[pageURL host] containsString:@"facebook.com"] || [[pageURL host] containsString:@"accounts.google"] || [[pageURL host] containsString:@"accounts.youtube"])){
            decisionHandler(WKNavigationActionPolicyCancel);
            [[UIApplication sharedApplication] openURL:webView.URL options:[NSDictionary dictionary] completionHandler:^(BOOL success) {}];
            [self.loadingSpinner setHidden:TRUE];
            return;
        }
    }
    
    if([[pageURL host] containsString:@"facebook.com"] && [[actionURL host] isEqualToString:[baseURL host]]){
        dispatch_async(dispatch_get_main_queue(),^{
            [self performSelector:@selector(toggleHome) withObject:nil afterDelay:0.5];
        });
    }
  

    if([[actionURL absoluteString] containsString:@"location=home"] ||
        [[actionURL absoluteString] isEqualToString:self.rootURLPath]){
        [self.tabBar setSelectedItem:[self.tabBar.items objectAtIndex:0]];
    }
    else if([[actionURL absoluteString] containsString:@"location=about"]){
        [self.tabBar setSelectedItem:[self.tabBar.items objectAtIndex:1]];
    }
    else if([[actionURL absoluteString] containsString:@"location=area"]){
        [self.tabBar setSelectedItem:[self.tabBar.items objectAtIndex:2]];
    }

    if ([[actionURL scheme] isEqual:@"mailto"] ||
        [[actionURL host] isEqual:@"twitter.com"]) {
        [[UIApplication sharedApplication] openURL:actionURL options:[NSDictionary dictionary] completionHandler:^(BOOL success) {}];
        decisionHandler(WKNavigationActionPolicyCancel);
        [self.loadingSpinner setHidden:TRUE];
        return;
    }

    if([[actionURL scheme] isEqualToString:@"about"]){
        decisionHandler(WKNavigationActionPolicyCancel);
        [self.loadingSpinner setHidden:TRUE];
        return;
    }

    // Abrir downloads fora da aplicação no browser
    if([[actionURL absoluteString] containsString:@"doc?id="]){
        UIApplication * app = [UIApplication sharedApplication];
        if ([app canOpenURL:actionURL]) {
            [app openURL:actionURL options:[NSDictionary dictionary] completionHandler:^(BOOL success) {

            }];
        }
        decisionHandler(WKNavigationActionPolicyCancel);
        [self.loadingSpinner setHidden:TRUE];
        return;
    }

    // this is a 'new window action' (aka target="_blank") > open this URL externally.
    // If we´re doing nothing here, WKWebView will also just do nothing. Maybe this will change in a later stage of the iOS 8 Beta
    if (!navigationAction.targetFrame) {
        NSURL *url = navigationAction.request.URL;
        NSString * tmpURLString = [url absoluteString];

        NSURL * tmpURL = [NSURL URLWithString:tmpURLString];

        UIApplication *app = [UIApplication sharedApplication];
        if ([app canOpenURL:tmpURL]) {
            [app openURL:tmpURL options:[NSDictionary dictionary] completionHandler:^(BOOL success) {

            }];
        }
    }
    
    decisionHandler(WKNavigationActionPolicyAllow);
}

- (void)webView:(WKWebView *)webView didReceiveServerRedirectForProvisionalNavigation:(WKNavigation *)navigation{
    NSString * urlStr = webView.URL.absoluteString;
    NSArray * items = [urlStr componentsSeparatedByString:@"?"];
    
    if([urlStr containsString:@"https://accounts.google.com/signin/oauth/consent"]){
        dispatch_async(dispatch_get_main_queue(),^{
            [self toggleArea];
        });
    }
    
    if([items count]>1){
        NSString * location = [items lastObject];
        [[NSNotificationCenter defaultCenter] postNotificationName:@"tabOpenPage" object:location];
    }
    
    // This fixes Webview redirect after login
    // for some reason, the webview doesn't reload the page.
    // The extra 2 seconds its for the logout, to avoid login in again.
    if([[webView.URL host] isEqualToString:[[NSURL URLWithString:self.rootURLPath] host]]){
        [webView performSelector:@selector(reload) withObject:nil afterDelay:2.0];
    }
}


-(NSString*)getToken{
    NSUserDefaults *userDefaults = [NSUserDefaults standardUserDefaults];
    if([userDefaults objectForKey:TOKEN_KEY] != nil){
        return [userDefaults objectForKey:TOKEN_KEY];
    }
    return nil;
}


-(void)saveToken:(NSString*)token{
    NSUserDefaults *userDefaults = [NSUserDefaults standardUserDefaults];
    if(token != nil && [userDefaults objectForKey:TOKEN_KEY] == nil){
        [userDefaults setObject:token forKey:TOKEN_KEY];
        [userDefaults synchronize];
    }
}

-(void)logout{
    self.jsessionId = nil;
    [self enablePhotoButton:FALSE];
    
    NSUserDefaults *userDefaults = [NSUserDefaults standardUserDefaults];
    [userDefaults removeObjectForKey:TOKEN_KEY];
    [userDefaults synchronize];
    
    NSHTTPCookieStorage *storage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    for (NSHTTPCookie *cookie in [storage cookies]) {
        [storage deleteCookie:cookie];
    }
    
    [[NSURLCache sharedURLCache] removeAllCachedResponses];
    [[NSUserDefaults standardUserDefaults] synchronize];
    
    WKWebsiteDataStore *dateStore = [WKWebsiteDataStore defaultDataStore];
    [dateStore fetchDataRecordsOfTypes:[WKWebsiteDataStore allWebsiteDataTypes]
                     completionHandler:^(NSArray<WKWebsiteDataRecord *> * __nonnull records) {
                         for (WKWebsiteDataRecord *record  in records)
                         {
                             
                             [[WKWebsiteDataStore defaultDataStore] removeDataOfTypes:record.dataTypes
                                                                       forDataRecords:@[record]
                                                                    completionHandler:^{
                                                                        dispatch_async(dispatch_get_main_queue(),^{
                                                                            [self toggleHome];
                                                                        });
                                                                    }];
                             
                         }
                     }];
}

-(NSURL*)pageURL{
    NSString * temp = [NSString stringWithString:self.rootURLPath];
    temp = [temp stringByAppendingString:self.tabPath];
    return  [NSURL URLWithString:temp];
}

- (void)loadRoot
{
    NSMutableURLRequest * request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:self.rootURLPath]];
    NSString * token = [self getToken];
    if(token != nil)
        [request setValue:token forHTTPHeaderField:@"tok"];
    [self.webView loadRequest:request];
}

-(void)loadURL:(NSURL*)url{
    NSString * tmpURLString = [url absoluteString];
    NSMutableURLRequest * request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:tmpURLString]];
    NSString * token = [self getToken];
    if(token != nil)
        [request setValue:token forHTTPHeaderField:@"tok"];
    [self.webView loadRequest:request];
}

-(void)toggleHome{
    self.tabPath = @"?location=home&mobileReq=true";
    [self loadURL:[self pageURL]];
}

-(void)toggleArea{
    self.tabPath = @"?location=area&mobileReq=true";
    [self loadURL:[self pageURL]];
}



- (void)readerSession:(nonnull NFCNDEFReaderSession *)session didInvalidateWithError:(nonnull NSError *)error
{
    NSLog(@"Error: %@", [error debugDescription]);
    
    if (error.code == NFCReaderSessionInvalidationErrorUserCanceled) {
    }
    
    self.session = nil;
}

- (void)readerSession:(nonnull NFCNDEFReaderSession *)session didDetectNDEFs:(nonnull NSArray<NFCNDEFMessage *> *)messages
{
    for (NFCNDEFMessage *message in messages)
    {
        for (NFCNDEFPayload *pl in message.records)
        {
//            const NSDate *date = [NSDate date];
            NSString * identifier = [[NSString alloc] initWithData:pl.identifier
                                  encoding:NSASCIIStringEncoding];
//            NSString * type = [[NSString alloc] initWithData:pl.type
//                                  encoding:NSASCIIStringEncoding];

            NSString * payload = [[NSString alloc] initWithData:pl.payload
                                                       encoding:NSASCIIStringEncoding];
            
            if([payload containsString:@"BF_P_"])
            {
                NSRange start = [payload rangeOfString:@"BF_P_"];
                NSRange end = [payload rangeOfString:@"_P_FB"];
                
                if(start.location == NSNotFound || end.location == NSNotFound )
                    continue;
                
                NSInteger startIdx =start.location+start.length;
                NSInteger endIdx = end.location;
                
                NSString * challenge_id = [payload substringWithRange:NSMakeRange(startIdx, endIdx-startIdx)];
                
                for(NSInteger i = 0;i<[self.challenges count];i++){
                    NSDictionary * entry = [self.challenges objectAtIndex:i];
                    
                    NSObject * idKey = [entry objectForKey:@"id"];
                    NSString * titleStr = [entry objectForKey:@"title"];
                    NSString * idStr = [NSString stringWithFormat:@"%@",idKey];
                    
                    if(![challenge_id isEqualToString:idStr])
                        continue;
                        
                    dispatch_async(dispatch_get_main_queue(),^{
                        self.challengeNFC = identifier;
                        
                        self.commentTitle.text = NSLocalizedString(@"PARTICIPATION_NFC_TITLE",@"");
                        self.commentText.text = [NSString stringWithFormat:NSLocalizedString(@"PARTICIPATION_NFC_BODY",@""),(int)[self.challengeNFC hash]];
                        
                        [self.challengeButton setTitle:titleStr forState:UIControlStateNormal];
                        self.challengePk = idStr;
                        
                        [self.pickChallengeTV setHidden:TRUE];
                        [self.commentWrapper setHidden:FALSE];
                            
                        [self.commentTitle setEnabled:TRUE];
                        [self.commentText setEditable:TRUE];
                        [self.sendComment setEnabled:TRUE];
                        
                        UIAlertController * alert = [UIAlertController alertControllerWithTitle: NSLocalizedString(@"PARTICIPATION_MSG_NFC_TITLE",@"") message:
                                                     NSLocalizedString(@"PARTICIPATION_MSG_NFC_MSG",@"") preferredStyle:UIAlertControllerStyleAlert];
                            
                        UIAlertAction* action = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK",@"") style:UIAlertActionStyleDefault
                                                                           handler:^(UIAlertAction * action) {
                                                                               [alert dismissViewControllerAnimated:TRUE completion:^{}];
                                                                           }];
                            
                        [alert addAction:action];
                            
                        [self presentViewController:alert animated:TRUE completion:^{}];
                    });
                        
                    break;
                }
                
                break;
            }
        }
    }
    
    [(NFCNDEFReaderSession*)self.session invalidateSession];
    self.session = nil;
}


- (IBAction)doScanOp:(id)sender {
    if(!self.nfcAvailable){
        UIAlertController * alert = [UIAlertController alertControllerWithTitle: NSLocalizedString(@"ERROR",@"") message:
                                     NSLocalizedString(@"PARTICIPATION_NFC_ERROR",@"") preferredStyle:UIAlertControllerStyleAlert];
        
        UIAlertAction* action = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK",@"") style:UIAlertActionStyleDefault
                                                       handler:^(UIAlertAction * action) {
                                                           [alert dismissViewControllerAnimated:TRUE completion:^{}];
                                                       }];
        
        [alert addAction:action];
        
        [self presentViewController:alert animated:TRUE completion:^{}];
        
        return;
    }
    
    self.session = [[NFCNDEFReaderSession alloc] initWithDelegate:self queue:self.nfc_dispatch_queue invalidateAfterFirstRead:NO];
    [(NFCNDEFReaderSession*)self.session beginSession];
}

@end
