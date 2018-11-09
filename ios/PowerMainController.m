
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

#import "PowerMainController.h"
#import "PowerCityCell.h"
#import "PowerCityController.h"
#import "Reachability.h"

@interface PowerMainController () <UITableViewDataSource,UITableViewDelegate>
@property(strong) Reachability * reachability;
@property(weak) UIAlertController * lastAlert;
@property(weak) UIViewController * presentedVC;
@end

@implementation PowerMainController

-(NSInteger)numberOfSectionsInTableView:(UITableView *)tableView{
    return 1;
}

-(NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section{
    return 4;
}

-(UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath{
    
    PowerCityCell * cell = [tableView dequeueReusableCellWithIdentifier:@"powercell"];
    if(!cell){
        [tableView registerNib:[UINib nibWithNibName:@"PowerCityCell" bundle:nil] forCellReuseIdentifier:@"powercell"];
        cell = [tableView dequeueReusableCellWithIdentifier:@"powercell"];
    }
    
    cell.selectedBackgroundView.backgroundColor = cell.backgroundColor;
    
    if(indexPath.row == 0){
        cell.deploymentName.text = @"Leicester";
        cell.deploymentImage.image = [UIImage imageNamed:@"leicester"];
    }
    else if(indexPath.row == 1){
        cell.deploymentName.text = @"Sabadell";
        cell.deploymentImage.image = [UIImage imageNamed:@"sabadell"];
    }
    else if(indexPath.row == 2){
        cell.deploymentName.text = @"Jerusalem";
        cell.deploymentImage.image = [UIImage imageNamed:@"jerusalem"];
    }
    else if(indexPath.row == 3){
        cell.deploymentName.text = @"Milton Keynes";
        cell.deploymentImage.image = [UIImage imageNamed:@"miltonkeynes"];
    }
    
    return cell;
}


-(void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath{
//    NSLog(@"%@ %@",tableView,indexPath);

    NSInteger rowId = indexPath.row;

    NSString * type = @"";

    if(rowId == 0)
        type = @"leicester";
    else if(rowId == 1)
        type = @"sabadel";
    else if(rowId == 2)
        type = @"jerusalem";
    else if(rowId == 3)
        type = @"milton";

    UIStoryboard * sb = [UIStoryboard storyboardWithName:@"Main" bundle:nil];
    PowerCityController * pcc = (PowerCityController*)[sb instantiateViewControllerWithIdentifier:@"PowerCityVC"];
    [pcc setPageType:type];

    pcc.modalTransitionStyle   = UIModalTransitionStyleCrossDissolve;
    pcc.modalPresentationStyle = UIModalPresentationFullScreen;
    
    self.presentedVC = pcc;
    
    [self presentViewController:pcc animated:TRUE completion:^{ }];
}

-(void)pressesBegan:(NSSet<UIPress *> *)presses withEvent:(UIPressesEvent *)event{

}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.cityTable.dataSource = self;
    self.cityTable.delegate = self;
    self.cityTable.separatorStyle = UITableViewCellSeparatorStyleNone;
    self.cityTable.rowHeight = 57;
    
    self.reachability = [Reachability reachabilityForInternetConnection];
    [self.reachability startNotifier];
    
    self.presentedVC = nil;
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleNetworkChange:) name:kReachabilityChangedNotification object:nil];
}

-(void)viewDidAppear:(BOOL)animated{

}

- (void) handleNetworkChange:(NSNotification *)notice
{
    NetworkStatus remoteHostStatus = [self.reachability currentReachabilityStatus];
    
    if(remoteHostStatus == NotReachable) {
        if(self.presentedVC){
            [self.presentedVC dismissViewControllerAnimated:TRUE completion:^{}];
            self.presentedVC = nil;
        }
        [self showUnreachableAlert];
    }
    else if (remoteHostStatus == ReachableViaWiFi) {
        [self hideAlert];
    }
    else if (remoteHostStatus == ReachableViaWWAN) {
        [self hideAlert];
    }
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
}

-(void)hideAlert{
     if(self.lastAlert) [self.lastAlert dismissViewControllerAnimated:TRUE completion:^{}];
}

-(void)showUnreachableAlert{
    if(self.lastAlert)
        [self.lastAlert dismissViewControllerAnimated:TRUE completion:^{}];
    
    UIAlertController * alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"REACHABILITY_TITLE",@"") message:NSLocalizedString(@"REACHABILITY_MSG",@"") preferredStyle:UIAlertControllerStyleAlert];
    
    UIAlertAction* action = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK",@"") style:UIAlertActionStyleDefault
                                                   handler:^(UIAlertAction * action) {
                                                       if(self.lastAlert)
                                                           [self.lastAlert dismissViewControllerAnimated:TRUE completion:^{}];
                                                       self.lastAlert = nil;
                                                   }];
    
    [alert addAction:action];
    
    self.lastAlert = alert;
}

@end
